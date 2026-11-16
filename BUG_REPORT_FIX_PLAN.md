# smptools — Bug Report & Fix Plan

**Project:** Minecraft Paper 1.21 plugin (Java 17, Maven), package `com.smp.smptools`, ~190 Java files.
**Plugin version:** 1.1 (`api-version: '1.21'` in `plugin.yml`).
**Build/test:** `mvn test` (21 test classes, all green — do not break them). Package with `mvn package`.

---

## 0. Scope & verification

A two-pass static review was performed. **Every** `.java` file was either read in depth or pattern-scanned (grep for unguarded parsing, `random.nextInt()` on possibly-empty collections, async/threading violations, config mutation, inventory/item handling). No files were modified. All bug locations below were re-verified against the current source.

**Behavioral conventions of this codebase (follow them in fixes):**
- Defensive config loading with per-entry try/catch is the established pattern (see `BountyManager.loadBounties()` — use it as the model for BUG 2/3).
- Async saves are done via `utils/AsyncConfigHelper` (FIFO) — use it for new config writes.
- All user-facing strings go through `MessageManager` (`messages.yml`) with `MiniMessage` — no hardcoded strings (there is one known violation, see BUG 4).
- YAML configs (`stats.yml`, `tags.yml`, `secretsanta.yml`, etc.) are `YamlConfiguration` and **must only be touched on the main thread** (see BUG 7 for the one violation).
- Main class: `SMPTools.java`; managers are wired in `onEnable`; `CommandRegistry`/`ListenerRegistry` register commands/listeners.

---

## 1. Bug summary

| # | Severity | Component | One-liner |
|---|----------|-----------|-----------|
| 1 | **HIGH (dupe)** | Secret Santa | Claimed gifts are never removed from `secretsanta.yml` → infinite `/ss claim` |
| 2 | **MEDIUM (crash)** | ChunkLoaders | Unguarded `Integer.parseInt` on config load → one bad line disables the whole plugin at startup |
| 3 | **MEDIUM (crash/dup rewards)** | Missions | Unguarded `parseInt` in reward parsing + mission marked claimed *after* rewards applied |
| 4 | **LOW (NPE)** | Sleep vote | `voteInitiator.getWorld()` NPE if initiator kicks; no `PlayerKickEvent` handler; hardcoded message |
| 5 | **LOW (perf)** | CropAccelerator | Wrong Y range (`0..maxHeight` vs actual min-Y) wastes ~17% lookups; saplings grow instantly |
| 6 | **LOW (item loss)** | Graves | Head placed one block up only — can overwrite solid blocks/water; drops cleared before placement succeeds |
| 7 | **MEDIUM (thread-safety)** | Chat | `AsyncChatEvent` handler reads `YamlConfiguration`s off the main thread |
| 8 | **LOW (cosmetic)** | /stats GUI | Death coordinates always show `0, 0, 0` (reads `location` sub-map that doesn't exist) |
| 9 | **LOW (world pollution)** | Krampus | Cage never removed; `releasePlayer` computes cage location after teleporting (wrong); kidnap state leaks on quit |
| 10 | **LOW (data-loss UX)** | Secret Santa | Deposit GUI saves on any close, no cancel/confirm → accidental deposits irretrievable |

---

## 2. Detailed findings & fix plans

### BUG 1 — HIGH: Secret Santa infinite claim (dupe)

**Files:** `commands/SecretSantaCommand.java`, `christmas/SecretSantaManager.java`

**Observed:** `/ss claim` (SecretSantaCommand lines ~91–104) fetches `manager.getGift(uuid)` and gives the items to the player, but nothing ever removes the stored gift. `SecretSantaManager` only ever does:
- line 120: `config.set("gifts." + target, items)` (write)
- line 125: `config.getList("gifts." + recipient)` (read)
- line 145: `config.contains("gifts." + target)` (check)

No code path calls `config.set("gifts.<uuid>", null)` or `saveConfig()` on claim. Result: a player can run `/ss claim` repeatedly and duplicate the gift items. Also, the claim path sends message key `secret-santa.gift-deposited` (wrong key — should be a "gift claimed" message).

**Fix plan:**
1. In `SecretSantaManager`, add a `claimGift(UUID recipient)` method that reads the gift list (like `getGift`), then atomically removes it: `config.set("gifts." + recipient, null)` + `saveConfig()` (via `AsyncConfigHelper` if one is used for this file), then returns the items (or a `null`/empty result when nothing stored).
2. In `SecretSantaCommand` claim path: call `claimGift`; if empty → "no gift" message and return; otherwise give items and send the correct message key (add e.g. `secret-santa.gift-claimed` to `messages.yml`).

**Regression:** no existing test for `SecretSantaManager` — add one that deposits, claims twice, and asserts the second claim yields nothing.

---

### BUG 2 — MEDIUM: ChunkLoader startup crash

**File:** `chunkloaders/ChunkLoaderManager.java` (lines 146–148)

**Observed:** `deserializeLocation()` does unguarded `Integer.parseInt(parts[1])`, `parts[2]`, `parts[3]` on lines split from `chunkloaders.yml`. It is called from `loadChunkLoaders()`, which runs from the constructor during `onEnable`. One corrupted/hand-edited line → `NumberFormatException` propagates → plugin fails to enable entirely.

**Fix plan:** wrap each entry in try/catch (model: `BountyManager.loadBounties()`), log a warning with the offending line, and continue loading the rest. Do the same for any other unguarded parsing in that class (check `worldName`/length handling too — `parts.length` should be validated before indexing).

---

### BUG 3 — MEDIUM: Mission reward parsing crash + reward ordering

**Files:** `missions/RewardManager.java` (line 91), `listeners/MissionGUIListener.java` (lines 196–200)

**Observed:** `RewardManager.giveReward()` does `int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;` for `item:` rewards. A config typo like `item:DIAMOND abc` throws `NumberFormatException`. In `MissionGUIListener.handleCompletedMissionsClick`, the claim flow is:

```java
for (String reward : clickedMission.getRewards()) {
    RewardManager.giveReward(player, reward);   // ← can throw
}
playerData.getClaimedMissions().add(clickedMission.getId());  // ← never reached
```

So an exception leaves the mission **unclaimed** and the player can re-trigger the claim — re-running any earlier `command:` rewards (duplicate rewards) and repeating until the server logs fill. It also propagates up the inventory event.

**Fix plan:**
1. Mark the mission claimed (`playerData.getClaimedMissions().add(...)` + save) **before** applying rewards; on a reward exception, still send the claimed message and log a warning.
2. Harden `RewardManager.giveReward()`: try/catch around the parse (and around each reward application), skip + warn on malformed rewards instead of throwing.

---

### BUG 4 — LOW: Sleep vote NPE on kick

**Files:** `sleep/SleepManager.java` (lines 92, 97–99), `listeners/SleepListener.java`

**Observed:** `checkVoteStatus()` dereferences `voteInitiator.getWorld()` (and later `setTime`/`setStorm` on it). If the initiator is **kicked** (or crashes) mid-vote, the vote is never cancelled: `SleepListener` handles only `PlayerQuitEvent`, not `PlayerKickEvent` → `voteInitiator` is offline but non-null → NPE on the next vote check, and the vote stays stuck forever. Also, `SleepListener` contains the hardcoded string `"A sleep vote is already in progress."` instead of a `messages.yml` key.

**Fix plan:**
1. Add a `PlayerKickEvent` handler (same cleanup as quit: remove initiator, cancel vote, reset world time if needed).
2. Null-guard `voteInitiator` / `voteInitiator.getWorld()` in `checkVoteStatus()` (also guard the periodic task — cancel it when initiator goes offline).
3. Move the hardcoded message into `messages.yml` (e.g., `sleep.vote-in-progress`) and use `MessageManager`.

---

### BUG 5 — LOW/perf: CropAccelerator Y-range + sapling overgrowth

**File:** `accelerators/CropAccelerator.java` (line 54)

**Observed:** every 20 ticks, for every loaded chunk, `random.nextInt(world.getMaxHeight())` picks Y in `0..383` on the overworld, but valid terrain Y is `-64..319` → ~17% of block reads hit the void/air ceiling pointlessly, and the scan then walks the column anyway. Separately, `applyBoneMeal` is applied to saplings too, so trees grow almost instantly (balance issue).

**Fix plan:**
1. Use `world.getMinHeight()`..`world.getMaxHeight()` (`nextInt(max-min)+min`) or restrict to `0..world.getHighestBlockYAt(x,z)` and stop the column scan at the first non-air block for efficiency.
2. Consider a per-tick budget (max blocks per tick) instead of scanning every chunk every 20 ticks, and/or gate bone-meal application to crops only (skip saplings) unless intentional.
3. The `multiplier` config should cap (e.g., clamp to 0..64) to avoid `applyBoneMeal` × huge values.

---

### BUG 6 — LOW: Grave placement item loss

**File:** `graves/GraveManager.java`

**Observed:** grave placement checks only one block above the death location. If that block is solid (or water/lava), the head/plates either overwrite a block or get placed in liquid; and `event.getDrops().clear()` runs **before** placement is confirmed — if placement fails, the items are gone (already cleared) with no fallback.

**Fix plan:**
1. Loop upward (e.g., up to 5 blocks) looking for a passable, non-liquid location; only then clear drops and place the grave.
2. If no valid spot is found, skip clearing drops entirely (items drop normally).
3. Never place a grave head inside a solid block — verify the target block and the one above are air/passable.

---

### BUG 7 — MEDIUM: Chat handler reads YAML off the main thread

**Files:** `listeners/ChatListener.java` (line 21/25), `chat/ChatManager.java` (lines 31–59)

**Observed:** `AsyncChatEvent` fires on a netty async thread. The handler calls `ChatManager.getFormattedDisplayName(player)`, which reads `plugin.getStatsConfig()` and `plugin.getTagsConfig()` (plain `YamlConfiguration`, not thread-safe) and calls `TagManager.getTagTitle/getTagDescription` (same configs). A concurrent main-thread save/mutation (periodic stats save, tag unlock) can cause `ConcurrentModificationException`/corruption in the async chat pipeline. This violates the codebase's own pattern: other subsystems explicitly hand off to the main thread (`callSyncMethod` in `TagManager`, `LeaderboardManager`, `TrailManager`).

**Fix plan (pick one, prefer the snapshot):**
1. **Snapshot (best):** build a `Map<UUID, Component>` of display names on the main thread (e.g., in the same task that updates nametags, or on demand via `Bukkit.getScheduler().callSyncMethod`), and have the async chat handler read the snapshot — zero chat latency impact.
2. Or route the whole handler through `Bukkit.getScheduler().runTask(...)` (accepts a tick of chat latency).
3. Whatever the choice, also make the broadcast from the main thread or use the existing broadcast pattern used elsewhere.

---

### BUG 8 — LOW/cosmetic: `/stats` death coordinates always 0,0,0

**Files:** `commands/StatsCommand.java` (`showDetailedDeathInfoGUI`), `listeners/StatsListener.java` (`onPlayerDeath`)

**Observed:** the death records are stored with top-level keys `x`, `y`, `z` (StatsListener writes `death.put("x", ...)` etc.), but the detail GUI reads `((Map) death.get("location")).get("x")` — `"location"` is never written, so the lookup is always null and the Coordinates row renders `X: 0, Y: 0, Z: 0`.

**Fix plan:** read `death.get("x")`, `death.get("y")`, `death.get("z")` directly (they are already `Number`). Optionally add a migration for old records (they already have the same shape, so a plain read fix is sufficient).

---

### BUG 9 — LOW: Krampus cage never cleaned up + kidnap state leaks

**File:** `christmas/KrampusManager.java`

**Observed:**
- `kidnapPlayer()` (lines 85–97) builds a 9×9×6 iron-bars cage with bedrock floor at `player.getLocation().add(0,50,0)` and spawns zombie guards. **Nothing ever removes the cage** — every kidnap permanently pollutes the world (structure persists across restarts).
- `releasePlayer()` (lines 121–130) teleports the player back to `originalLoc` (line 126) **before** computing `cageLoc = player.getLocation().add(0,50,0)` (line 130) — so even the (non-existent) cleanup would target the wrong location. The source comment even flags this: "Logic needs to track cage loc if moving".
- `kidnappedPlayers`/`playerGuards` are memory-only with no `PlayerQuitEvent` handler: if a kidnapped player logs out, they rejoin still flagged as kidnapped and their guards keep roaming indefinitely.

**Fix plan:**
1. Store the cage origin per player (e.g., `Map<UUID, Location>`), and on release (or on quit) remove the cage blocks (iterate the 9×9×6 region, restore air except the bedrock floor — or store original blocks and restore them).
2. Move the cage-location computation **before** the teleport (or use the stored location).
3. Add a quit handler: release the player + clean up cage + despawn guards on `PlayerQuitEvent`.

---

### BUG 10 — LOW/data-loss UX: Secret Santa deposit is a one-way trap

**File:** `commands/SecretSantaCommand.java` (`openDepositGUI` line 133, `onInventoryClose` line 143)

**Observed:** the deposit GUI finalizes (`manager.depositGift(...)`) on **any** inventory close — including accidental close or misclick. There is no cancel/confirm. Once deposited, items are unrecoverable: they can only be claimed by the recipient during CELEBRATION, and are never returned if the recipient is inactive. Combined with BUG 1, the stored gift is also never removed after claim (same `gifts.` state).

**Fix plan:** add an explicit "Deposit" confirm button (and/or a cancel path that discards without saving); only call `depositGift` on confirm. Optionally validate the recipient is still a registered participant before accepting the deposit. (Ties into BUG 1's `claimGift` for the removal side.)

---

## 3. Minor observations (fix at your discretion — not bugs per se)

- `listeners/TeleportListener.java`: logs `INFO` on every teleport cancellation (move/damage) — log spam on active servers; lower to `FINE`/`DEBUG`.
- `skills/SkillsManager.java` (treasure hunt): `random.nextInt(rare.size())` with an empty loot list throws — it is caught, so the hunt silently never rewards; add an explicit empty-list guard + warning.
- `managers/NPCManager.java`: `npcRegistry` is not cleared on reload → stale NPC entries after `/npc reload`.
- `listeners/FestiveMobsListener.java`: dead code — `candyCane.getItemMeta().displayName(name)` result is discarded (must `setItemMeta`).
- `music/MusicCommand.java`: `music broadcast` uses a player snapshot → players who join mid-song don't hear it (by design? verify).
- `tags/TagManager.checkMilestones()`: takes `max(config, db)` so stale YAML can't be overwritten by DB — confirm this is intentional.
- `skills/StatsListener` etc.: skills/stats still write `stats.yml` directly even when a DB provider is configured (DB is used only for leaderboards/tags/trails).

## 4. Confirmed solid (do NOT "fix" these)

- Trade system (`TradeSession`/`TradeListener`): slot restrictions, close/quit cancellation, synchronized complete/cancel — dupe-safe.
- Bounty, locks, teleport/TPA, missions tracker, storage providers (`FlatFile`/`Jdbc`/`Mongo`) and `StorageManager` shutdown.
- `AsyncConfigHelper` (FIFO async saves), `URLValidator`/`BoundedInputStream` (SSRF guards), `InputValidator` + MiniMessage sanitization, `CommandBlacklist`, `BoundedInputStream`.
- `LumberjackEnchant`/`TelekinesisEnchant` interaction (tree-break + auto-pickup) — analyzed, no dupe path.
- `plugin.yml`/`CommandRegistry` — all 52 commands resolve.

## 5. Implementation order & testing

Suggested order: **BUG 1 → 3 → 2 → 7** (exploit/crash/threading) → **4 → 6 → 9 → 10** (robustness) → **5 → 8** (perf/cosmetic) → minor notes.

- Run `mvn test` after each change; all 21 existing tests must stay green.
- Existing tests relevant to touched areas: `LockManagerTest`, `BountyManagerTest`, `TradeManagerTest`, `MessageManagerTest`, `InputValidatorTest`, `AsyncConfigHelperTest`, `StorageShutdownTest`.
- Add new tests for: `SecretSantaManager.claimGift` (claim twice → second empty), `RewardManager.giveReward` with malformed reward strings (no throw), `ChunkLoaderManager.deserializeLocation` with garbage input (no throw).
- Manually smoke-test on a Paper 1.21 server: `/ss deposit` + `/ss claim` twice; corrupt one line in `chunkloaders.yml` and restart; claim a mission with a deliberately malformed reward; initiate a sleep vote and kick the initiator; chat while a stats save happens.

# smptools — Pass 2 Bug Report & Fix Spec (for the fixing agent)

**Project:** Minecraft Paper 1.21 plugin (Java 17, Maven), package `com.smp.smptools`, ~203 Java files.
**Plugin version:** 1.1 (`api-version: '1.21'`, branch `updates/1.1`).
**Build/test:** `mvn -q test -o` — all tests PASS and compile is clean. Do not break them.
**Context:** Pass 1 (`BUG_REPORT_FIX_PLAN.md`) covered the pre-existing systems. Pass 2 covers the three NEW feature systems (artifacts, seasonal events, mini-events) and the fix code added in commits `986a531`→`f466bc2`. This document is a **historical snapshot of issues identified specific to revision `f466bc2`** — everything else attributed to Pass 2 has already been fixed and verified (see §4).

---

## 0. Rules for the fixing agent

1. Do not touch anything in §4 ("Verified solid — do not touch") unless a fix below requires it.
2. Keep the codebase conventions: all user-facing strings stay as-is (these subsystems intentionally bypass `MessageManager` — do NOT refactor that here); YAML configs are main-thread only; new scheduled tasks must be cancelled on disable. Use the existing defensive patterns already present in the code (try/catch + warn, rollback-on-save-failure, leftover `addItem` → `dropItemNaturally`).
3. Only fix the items in §2/§3. No refactors, no renames, no unrelated cleanup.
4. After each change run `mvn -q compile -o` and `mvn -q test -o`. All existing tests must stay green.
5. Do not touch the `BUG_REPORT_FIX_PLAN.md` file.

---

## 1. Remaining-open bug summary (current HEAD: `f466bc2`)

| # | Severity | Component | One-liner |
|---|----------|-----------|-----------|
| N1 | **MEDIUM (crash cascade)** | Artifacts | Magnet Totem `normalize()` on zero vector throws every second → Bukkit permanently cancels the whole passive-effects task |
| N2 | **MEDIUM (exploit)** | Artifacts | Phoenix Feather resurrects infinitely — no cooldown, no consume → undying players |
| N3 | **MEDIUM** | Artifacts | Vampiric Scythe / Sonic Horn damage & knock back other players, pets, villagers through walls, no friendly-fire/PvP/LoS check |
| N4 | **LOW** | Seasonal | Hunt progress keyed only by UUID — never resets between event years (event is one-time-per-player forever) |
| N5 | **LOW** | Seasonal | `total_pumpkins: 0` / `total_eggs: 0` in `seasonal.yml` unlocks the grand reward with zero progress |
| N6 | **LOW (item loss)** | Seasonal | `/halloween setpumpkin` overwrites ANY looked-at block with `JACK_O_LANTERN` (chest/barrel contents destroyed, no confirmation) |
| N7 | **LOW (leak)** | Seasonal | `trickOrTreatCooldown` map entries never removed |
| N8 | **LOW** | Mini-events | `events.interval-minutes: 0` or negative → auto-scheduler runs once at boot then never repeats, no warning |
| N9 | **LOW** | Mini-events | `events.types.double_xp.multiplier` config is dead; DOUBLE_XP has no scoring hooks (empty scoreboard, no winners); plus other dead config keys |
| N10 | **LOW** | Mini-events | Offline rewards: failed deliveries retried forever on every join; failed deliveries for ONLINE winners are silently lost (never queued) |
| N11 | **LOW (balance/data)** | Artifacts | Timber Axe logs broken via `breakNaturally(tool)` never consume tool durability; hand-fired `BlockBreakEvent`s double-count stats/mini-event points |
| N12 | **LOW (perf)** | Mini-events | EventGUI rewrites all 27 slots per viewer every second; refresh task restarted on every GUI open |
| N13 | **LOW (cosmetic)** | Seasonal | Hub GUI + `/summer` hardcode date ranges that are now config-driven |

---

## 2. Detailed findings & fix plans (NEW code fix work)

### N1 — MEDIUM (crash cascade): Magnet Totem zero-vector `normalize()` kills all passive effects server-wide

**File:** `src/main/java/com/smp/smptools/artifacts/ArtifactListener.java:481`

```java
if (entity instanceof Item || entity instanceof ExperienceOrb) {
    entity.setVelocity(player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.4));
}
```

**Why it's a bug:** an item/XP orb at exactly the player's position produces a zero vector; `Vector.normalize()` throws `IllegalArgumentException`. The exception is uncaught inside the repeating `runTaskTimer(plugin, 20L, 20L)` passive task (line 598) — Bukkit removes a scheduled task on an uncaught exception, so **all** passive artifact effects (Abyssal Lantern, Magnet, Auto-Feeder, Chlorophyll Band, Pumpkin Helmet, Ore Radar, Homing Compass) silently stop working for the entire server until reload.

**Fix plan:**
1. Guard the vector: skip entities whose distance to the player is ~0 (`if (entity.getLocation().distanceSquared(player.getLocation()) < 0.001) continue;`), OR only apply when the resulting vector length > 0.
2. As belt-and-braces, wrap the per-player passive block (or the whole task body) in try/catch and log a warning instead of letting an exception kill the task (matches the codebase's defensive pattern).

**Verify:** equip Magnet Totem, stand exactly on a dropped item, wait ≥2 ticks — no exception in console, other passive effects keep working. Also check with an XP orb.

---

### N2 — MEDIUM (exploit): Phoenix Feather = infinite resurrection

**File:** `src/main/java/com/smp/smptools/artifacts/ArtifactListener.java:287-298`

```java
if (player.getHealth() - event.getFinalDamage() <= 0) {
    if (artifactManager.hasEquippedArtifact(player, ArtifactType.PHOENIX_FEATHER)) {
        event.setCancelled(true);
        player.setHealth(player.getMaxHealth() * 0.5);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 4));
        ...
```

**Why it's a bug:** no cooldown and the artifact is never consumed. Any player with it in the pouch resurrects on every lethal hit (attack, fall, fire, explosion) with 50% HP + Regen V. In the void, the void-damage check + this branch keeps resurrecting every tick — effectively immortal. Compare: Minecraft's vanilla totem is consumed per use.

**Fix plan (pick one — do NOT make this artifact consumable, the pouch has no consume flow):**
1. Add a per-player cooldown map (e.g. `Map<UUID, Long> phoenixCooldowns`, 60s) — after a resurrection, further lethal hits in that window deal damage normally (and the player can die for real).
2. OR limit resurrections per session (e.g. one per 5 minutes) with an action-bar message when blocked.
3. Also ensure VOID damage with the feather equipped does NOT resurrect repeatedly: on `DamageCause.VOID`, either skip the Phoenix branch entirely (Void Saver already handles void rescue) or let the player actually die once past cooldown.

**Verify:** two players with/without cooldown: lethal hit → resurrection once, second lethal hit within the window → death. Cleanup the cooldown entry on `PlayerQuitEvent` (pattern already exists at lines 76-89).

---

### N3 — MEDIUM: Scythe & Sonic Horn damage/kb all entities, no friendly-fire check

**File:** `src/main/java/com/smp/smptools/artifacts/ArtifactListener.java:148-168` (VAMPIRIC_SCYTHE), `:170-186` (SONIC_WAVE_HORN)

```java
for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
    if (entity instanceof LivingEntity target && target != player) {
        double damage = target.getHealth() * 0.15;
        target.damage(damage, player);
```

**Why it's a bug:** `getNearbyEntities(8/5)` includes OTHER PLAYERS, pets, wolfs, villagers — with no `Player` exclusion, no PvP check, no line-of-sight check. On a SMP this is a grief tool (drain + knock players around, out of claim-triggered combat if the server relies on `EntityDamageByEntityEvent` cancellations — note `LivingEntity.damage(amount, entity)` still fires that event correctly, but plain Paper survival grants it anyway).

**Fix plan:**
1. Skip `Player` targets in both loops (`if (target instanceof Player) continue;`) — or better: engage gameplay-appropriate checks: skip players with no PvP negotiation, skip tamed/pet entities (`target.isTamed()` / `target.getOwner() != null`), optionally require line-of-sight (`player.hasLineOfSight(target)`).
2. Consider also skipping passive animals (not monsters) for the scythe (design decision — the description says "enemies"; at minimum players must be excluded).
3. Keep the rest (cooldown, heal) unchanged.

**Verify:** two players, one with the scythe/horn — no damage/knockback applied to the other player; hostile mobs still affected.

---

### N4 — LOW: Seasonal progress never resets between years

**File:** `src/main/java/com/smp/smptools/events/seasonal/SeasonalManager.java:215, 238-240, 287-289, 338, 361-363, 418-420`

All paths are `players.<uuid>.halloween.found`, `players.<uuid>.halloween.claimed_grand` (and `.easter.*`) — no year component.

**Why it's a bug:** a player who completed Halloween 2025 has `claimed_grand=true` + a full `found` list forever — Halloween 2026 offers them nothing (see N5's sibling check in the claim methods, which use `getFoundPumpkins` without year awareness).

**Fix plan (minimal, backwards-compatible):**
1. Add the year into the path at **read and write** sites: e.g. `players.<uuid>.halloween.2026.found` computed via `ZonedDateTime.now(zone).getYear()` / `MonthDay`-based start (use the season's start year), consistent with `getCurrentSeason()`.
2. On read, fall back to the old yearless key once (migration): if the year-key is absent but the yearless key exists, treat the player as having NOT completed the current year (fresh start) — do not carry over progress.
3. Make sure BOTH the GUI code calling these getters (`HalloweenGUI`/`EasterGUI`, `getFoundPumpkins`/`hasClaimed*`) and the claim methods use the same year-bearing path, so year change = fresh hunt + renewable grand reward.

**Verify:** unit-testable if you inject dates; otherwise smoke: set a fake completed state under the old key, ensure the current-year lookup ignores it.

---

### N5 — LOW: `total_pumpkins: 0` / `total_eggs: 0` unlocks the grand reward instantly

**File:** `src/main/java/com/smp/smptools/events/seasonal/SeasonalManager.java:277` (`if (found.size() < total)`) and `:408` (same for eggs). Totals come from `plugin.getSeasonalConfig().getInt("seasonal.halloween.total_pumpkins", 20)` (line ~269) / `seasonal.easter.total_eggs` (~400).

**Why it's a bug:** a config typo (`total_pumpkins: 0`, or negative) makes `found.size() < total` false → the grand reward (artifact + 16 diamonds) is claimable with zero progress, forever (once claimed_grand is set, per player).

**Fix plan:**
1. Clamp totals at read time: `int total = Math.max(1, config.getInt(...))` at both sites.
2. Optional belt-and-braces: also guard `find.size() < 1` — but clamping is sufficient.
3. Same treatment for any other count configs read in these methods (`mini_reward_*` already guarded with `> 0`).

**Verify:** set `total_pumpkins: 0` → claim is refused with the "only found x/y pumpkins" message; set it back to 20 → works as before.

---

### N6 — LOW (item loss): `/halloween setpumpkin` overwrites any looked-at block

**File:** `src/main/java/com/smp/smptools/events/seasonal/commands/HalloweenCommand.java:85`

```java
target.setType(Material.JACK_O_LANTERN);
seasonalManager.setPumpkinLocation(id, target.getLocation(), hint);
```

**Why it's a bug:** an admin looking at a full chest (or barrel, beacon, container) and running `/halloween setpumpkin <id>` replaces the block via `Block.setType()`, which destroys the tile entity WITHOUT dropping contents. No confirmation, no block-type sanity check. (The egg hunt does not have this problem — `EasterCommand` only records the location.)

**Fix plan:**
1. Before overwriting, reject non-replaceable types: return an error if `target.getState() instanceof Container` (or generally `target.getState() instanceof TileState`), and/or refuse when `!target.getType().isAir() && target.getType() != Material.JACK_O_LANTERN && target.getType() != Material.CARVED_PUMPKIN` unless the block is clearly decorative (grass, stone…). Simplest safe rule: **only allow setting targets on air or already-pumpkin blocks**; otherwise ask admins to place a carved pumpkin first.
2. Optionally add a confirmation step (`/halloween setpumpkin <id> confirm`) — but the type guard alone fixes the item-loss bug.

**Verify:** look at a chest, run the command → rejected with message, chest intact + contents intact; look at an air block → converts fine.

---

### N7 — LOW (memory leak): `trickOrTreatCooldown` never cleaned

**File:** `src/main/java/com/smp/smptools/events/seasonal/SeasonalListener.java:32, 158, 162`

`Map<UUID, Long>` — entries added on every trick-or-treat, never removed.

**Fix plan:**
1. Add a `PlayerQuitEvent` handler that removes the UUID (the class has no quit handler today — add one; it already may be the right place for nothing else).
2. Optionally prune stale entries older than, say, 24h during the villager-interact handler.

**Verify:** trick-or-treat, quit, rejoin — the map no longer holds the entry (or: server uptime memory stays flat).

---

### N8 — LOW: `events.interval-minutes: 0` / negative silently disables auto-scheduling

**File:** `src/main/java/com/smp/smptools/events/EventManager.java:149-150`

```java
int intervalMinutes = plugin.getEventsConfig().getInt("events.interval-minutes", 120);
long periodTicks = intervalMinutes * 60 * 20L;
```

`runTaskTimer` with a non-positive period is a one-shot (runs once at boot, never repeats). Also watch overflow on huge values.

**Fix plan:**
1. Clamp: `int intervalMinutes = Math.max(1, plugin.getEventsConfig().getInt("events.interval-minutes", 120));` and `long periodTicks = Math.max(1200L, (long) intervalMinutes * 60 * 20L);`
2. If the configured value was invalid, log a warning.

**Verify:** set `interval-minutes: 0` → normal 2h default behavior with a warning; event still auto-starts once at boot (existing behavior).

---

### N9 — LOW: DOUBLE_XP config is dead + no scoring hooks; several other dead config keys

**Files:** `src/main/java/com/smp/smptools/skills/SkillsManager.java:57` (`amount = amount * 2;` — hardcoded), `src/main/java/com/smp/smptools/events/minievents/MiniEventListener.java` (no DOUBLE_XP handler), resources `events.yml` (`double_xp.multiplier: 2.0`, `ore_rush.buffs.double-mining-xp: true`, `harvest_sprint.points.golden_apple_crop: 10`), `seasonal.yml` (`easter.golden_bunny_spawns: true`).

**Why it's a bug:** admins editing `events.yml`/`seasonal.yml` get zero effect for these keys. During a DOUBLE_XP event (configured 60 min), the scoreboard/top-list is always empty, `end()` announces "No players scored points", and no one can win rewards. The XP ×2 effect itself doesn't read the configured `multiplier`.

**Fix plan (minimal):**
1. In `SkillsManager.addExperience`, read `plugin.getEventsConfig().getDouble("events.types.double_xp.multiplier", 2.0)` instead of the hardcoded `2` (guard null `getEventsConfig()` not needed — manager exists when events enabled; keep the existing `getEventManager()` null-guard).
2. **Either** add a real scoring hook for DOUBLE_XP (e.g. `addPoints` on experience gains, fed from the same `addExperience` chokepoint), **or** — simpler and honest — document in `events.yml` that Double XP events bestow no points and auto-select it out of "scoring" events, so players aren't misled by an empty leaderboard. Pick the cheaper correct option; if a scoring hook is chosen, keep point values configurable under `events.types.double_xp.points.*`.
3. Either wire or remove the three dead keys from §-list: `double-mining-xp` (wire to a `SkillsManager` XP boost during ORE_RUSH or delete the key), `golden_apple_crop` (remove from `events.yml`), `golden_bunny_spawns` (remove from `seasonal.yml`). Removing keys from shipped resource files is fine.

**Verify:** with `double_xp.multiplier: 3.0`, skill XP during a Double XP event triples; leaderboard either shows activity or clearly states the event type gives no points.

---

### N10 — LOW: offline-reward delivery loop + silent online loss

**Files:** `src/main/java/com/smp/smptools/events/EventManager.java:74-99` (`claimOfflineRewards`), `:101-141` (`executeReward`), `src/main/java/com/smp/smptools/events/minievents/MiniEventSession.java:272-312` (`deliverRewards`).

**Why it's a bug:**
- `claimOfflineRewards` removes an entry from `pending_rewards.<uuid>` ONLY when `executeReward` returns true. A failing reward (unknown material, `cmd:` that errors, bad format) stays queued and is retried + re-warned on EVERY join forever; the player also never gets the "received" message.
- In `deliverRewards`, rewards for **online** winners are executed once and, on failure, dropped silently — not queued.

**Fix plan:**
1. In `claimOfflineRewards` and `executeReward`: reliably classify reward execution outcomes into success, permanent failure, and transient failure. Permanent failures (e.g. unknown material names, malformed reward syntax, invalid quantities) must be logged once with a warning for administrator review and permanently purged from the queue so they do not loop endlessly on every join. Transient failures (e.g. external command execution temporary errors or downstream subsystem unavailability) remain in the pending queue to stay recoverable across future joins.
2. In `deliverRewards`: for online players, distinguish execution failures similarly: queue transiently failed rewards into `pending_rewards.<uuid>` for subsequent retry on reconnect, while logging permanent failures for administrator review rather than dropping them silently.
3. Keep the `getStringList`/set-null semantics (lines 82-94) working; note `remaining.remove(reward)` removes one occurrence even when duplicates exist — that's fine/intended.

**Verify:** put a malformed reward in `events.rewards.participation`, end an event while offline → join → exactly ONE warning + no infinite retry.

---

### N11 — LOW (balance/data): Timber Axe durability never consumed + synthetic events double-count

**File:** `src/main/java/com/smp/smptools/artifacts/ArtifactListener.java:355-394`

```java
BlockBreakEvent subEvent = new BlockBreakEvent(current, player);
Bukkit.getPluginManager().callEvent(subEvent);
if (subEvent.isCancelled()) { continue; }
current.breakNaturally(tool);   // never burns tool durability, fires no real break
```

**Why it's a bug:**
1. `Block.breakNaturally(ItemStack)` does not apply durability loss, so a Netherite Timber Axe never breaks.
2. The hand-fired `BlockBreakEvent` re-enters this plugin's own listeners: `MiniEventListener.onBlockBreak` (ORE_RUSH/HARVEST_SPRINT/TREASURE_DIG MINI events award points per synthetic break — felling a tree during ORE_RUSH scores points per log) and `StatsListener` "blocks broken" stats count the synthetic logs.

**Fix plan:**
1. Durability: after each `breakNaturally(tool)` (or once per tree), call `player.getInventory().getItemInMainHand()` and apply damage only on real-vanilla semantics — simplest: `tool.damage(1, player)` guarded by an unbreaking-friendly formula, or check `Tool.hasEnchant(UNBREAKING)` and only damage when `random.nextInt(level+1) == 0` (match vanilla). Keep the axe from breaking the base log too.
2. Optionally, make the synthetic events only carry the spawn-relevant handlers: filter by setting a flag on the event so this plugin's own listeners skip it (e.g. reuse `currentlyFelling`: add the block BEFORE `callEvent` so `MiniEventListener`/stats skip? — they don't check it; simplest: pass `ignoreCancelled` (already) plus have the ORE_RUSH handler skip when... cleaner: only award points in `MiniEventListener.onBlockBreak` if `!currentlyFelling.contains(...)` — but that set is private to ArtifactListener; expose a static/package accessor or re-think: simplest correct = make the felling NOT fire synthetic events; instead break logs with `setType(AIR)` + `breakNaturally` without the event, then rely on the drops (losing protection-plugin veto ability, acceptable for a balance fix; keep protection by checking `world.getWorldGuard...` — keep it simple: keep `callEvent` for vetoing but recall stats/mini-events use a shared "ticks felling" guard).
3. Decide on the simplest correct approach: keep the event for veto, but have `onBlockBreak` (ArtifactListener) skip re-entry (already does), and make BOTH `MiniEventListener.onBlockBreak` and the stats-break listener skip blocks whose break is synthetic — achievable with a package-visible static `ArtifactListener.isFelling(Block)` helper.

**Verify:** timber-axe a tree of 20 logs → tool durability reduced (per unbreaking rules), ORE_RUSH points gained only for the base log, stats counter +1 not +20.

---

### N12 — LOW (perf): EventGUI full rewrite every second per viewer

**File:** `src/main/java/com/smp/smptools/events/gui/EventGUI.java:42-70` (refresh task) + `:80-135` (`updateInventoryContents`)

**Why it's a bug:** every viewer's 27 slots are re-set every 20 ticks (27 slot-update packets/second/viewer) even when only the timer text/leaderboard changed; `ensureRefreshTask()` also spawns a fresh repeating task on every GUI open while an old one is still ticking (it returns if alive, so only one runs — but then the "restart" path is dead code; the real cost is the unconditional full rewrite).

**Fix plan (keep it minimal):**
1. In the refresh task, only update the 3 dynamic slots when their content actually changed (compare rendered components/strings before `setItem`), or refresh at most every 2-3 seconds instead of every second.
2. Remove the redundant restart path if it exists (it's harmless; skip if no-op).

**Verify:** open the GUI with a live event — packet rate flat (no per-second 27-slot spam), timer/leaderboard still updates.

---

### N13 — LOW (cosmetic): hardcoded date ranges in hub & /summer

**Files:** `src/main/java/com/smp/smptools/events/seasonal/gui/SeasonalGUI.java:59` (`"<gray>Dates: Oct 15 - Nov 2</gray>"`), `:67` (`"Mar 25 - Apr 25"`); `src/main/java/com/smp/smptools/events/seasonal/commands/SummerCommand.java:28` (`"INACTIVE (Active Jul 1 - Aug 31)"`).

**Why it's a bug:** dates are now config-driven (`seasonal.dates.*`), but these display strings are hardcoded — an admin reconfiguring the calendar sees stale text in the hub card and /summer status.

**Fix plan:** expose the configured `MonthDay[]` range per season from `SeasonalManager` (the cache already holds them at `SeasonalManager.cachedDateRanges` — add a getter like `getSeasonDateRange(SeasonType)` and format it `MM-dd`) and use it in `SeasonalGUI` (Halloween/Easter cards) and `SummerCommand`. Add a `golden_bunny`-style reminder: keep the GUI lore otherwise untouched.

**Verify:** change `seasonal.dates.halloween.start` to "10-01" → hub card shows the new range.

---

## 3. Open items from Pass 1 still present (do NOT re-verify unless told; listed for completeness)

- `BUG_REPORT_FIX_PLAN.md` items 1-10 — **none were fixed in the recent commits**; they remain open. This Pass 2 spec does not cover them; the other agent should not touch them in this work session unless explicitly asked.

---

## 4. Verified solid in the current tree — DO NOT TOUCH (protects against regressions)

- **Wiring/lifecycle:** all three feature toggles consistent (`config.yml` ↔ `ConfigDefaults` ↔ `SMPTools.java`); managers created under correct toggles; `onDisable` ordering sound (tasks cancelled → `eventManager.shutdown()` → saves); no double listener registration; `EventManager.initialize()` registers `MiniEventListener` once.
- **Seasonal:** season gating on all 4 discovery/claim entry points; `addItem` leftovers dropped everywhere; save-failure rollback in discovery/claims; timezone + date-range config validation with fallbacks; in-memory location cache (no `requireNonNull` NPE); target blocks protected from break/explode/piston/burn/fade; `claimed_grand` set before item grant (no double-claim).
- **Mini-events:** overflow-safe duration math; `duration <= 0` rejected; scoreboards restored to original in `end()`; BossBar per-viewer API (no leak); hostile-only mob scoring; offline rewards queued atomically & claimed on join with leftover-drop; no async-thread YAML access; GUI click/drag canceled, no close-handler side effects; refresh task self-cancels when empty.
- **Artifacts:** per-type cooldowns with quit-cleanup; GUI validates shift-click/drag/hotbar/offhand and PASSIVE-only equips; `/artifacts give` leftovers dropped; pouch save on close keeps only passive artifacts; Void Saver, Leap Frog Boots (PDC-keyed flight grant/revoke on quit/join/move), Feather Glider, Ore Radar, Homing Compass all implemented.
- **Tests:** `mvn -q test -o` green (21 classes including modified `SeasonTypeTest`).
- Compile: `mvn -q compile -o` clean.

---

## 5. Suggested implementation order

**N1 → N2 → N3** (crash/exploit — highest risk) → **N5 → N6** (config/UX item loss) → **N4** (year reset — requires path changes, do it before N5 confirmation tests) → **N10 → N8 → N9** (robustness/dead config) → **N7 → N11 → N12 → N13** (leaks/perf/cosmetic).

Run `mvn -q compile -o` + `mvn -q test -o` after **every** change; keep all 21+ existing tests green. For N4/N9, add focused unit tests if the code is testable (e.g. a `SeasonalManager` year-path test with injected dates, an `EventManager.executeReward` malformed-input test asserting no-throw and warn-and-skip).
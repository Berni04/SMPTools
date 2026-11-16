# SMPTools Security & Bug Audit Report

| | |
|---|---|
| **Date** | 2026-08-21 |
| **Code revision audited** | git `02a50d9` (working tree) |
| **Scope** | All of `src/main/java` (~182 files, ~24k lines), `plugin.yml` |
| **Method** | 5 parallel subsystem audits (storage; URL-fetch/admin commands; locks/chunkloaders/portals; core/misc systems; christmas events) + manual line-by-line review of trade, bounty, graves, artifacts, mini-events, seasonal systems |
| **Purpose of this doc** | Hand-off sheet for a second reviewing agent. Every finding includes location, evidence, exploit/failure scenario, suggested fix, and **verification steps** so each claim can be independently confirmed or rejected. |

---

## 1. How to use this document (for the reviewing agent)

1. For each finding: re-open the cited file/lines at revision `02a50d9`, confirm the code excerpt matches, then walk the "Verification steps".
2. Mark each finding: CONFIRMED / PARTIALLY CONFIRMED (explain delta) / REJECTED (explain why) / NEEDS DYNAMIC TEST.
3. Findings marked confidence **Verified** were re-read by the original auditor directly; confidence **High** = subagent quoted exact code; **Medium** = reasoning-dependent or needs runtime testing.
4. Line numbers refer to the working tree at audit time; if code moved, locate by symbol names given.
5. Do not trust severity blindly - re-rate after verification. Severity assumes a public SMP server with untrusted players.

## 2. Classification legend

**Severity:** CRITICAL (server crash, mass theft, economy breakage, any-player-triggerable) / HIGH (dupes, permanent item loss, privilege escalation, persistent grief) / MEDIUM (bounded abuse, softlocks, bypasses requiring specific conditions) / LOW (leaks, perf, edge cases, UX).

**Category:** Theft/Grief, Item Duplication, Item Loss, DoS/Crash, SSRF/Network, Privilege Escalation, Injection, Race Condition, Data Loss/Corruption, Resource Leak, Protection Bypass, Logic Bug, Performance, Design/Balance.

**Confidence:** Verified / High / Medium.

## 3. Findings summary

| ID | Severity | Title | Category | Confidence |
|----|----------|-------|----------|------------|
| CRIT-01 | CRITICAL | Double-chest lock migration transfers ownership to stale-lock holder | Theft/Grief | Verified |
| CRIT-02 | CRITICAL | `lockContainer()` overwrites ownership; locking own half claims neighbor's half | Theft/Grief | High |
| CRIT-03 | CRITICAL | Image decompression bomb -> remote OOM via `/tomap` | DoS/Crash | Verified |
| CRIT-04 | CRITICAL | SSRF: DNS-rebinding TOCTOU + missing blocked IP ranges | SSRF/Network | High |
| HIGH-01 | HIGH | Trade click/drag handlers lack `ignoreCancelled` -> dupe via later cancel | Item Duplication | Verified |
| HIGH-02 | HIGH | Trade GUI stays interactive 1 tick after completion/cancel; decoration theft | Logic Bug | Verified |
| HIGH-03 | HIGH | Graves orphaned on head break/explosion; replacement-head looting | Theft/Grief | Verified |
| HIGH-04 | HIGH | Grave keyed by Location: second death same spot silently destroys items | Item Loss | Verified |
| HIGH-05 | HIGH | Vault decode failure permanently destroys contents; unbounded `readInt()` OOM | Item Loss / DoS | High |
| HIGH-06 | HIGH | Chunk-loader listeners ignore cancellation -> phantom loaders + beacon dupe | Item Duplication | High |
| HIGH-07 | HIGH | Chunk loaders: no cap, no explosion handling, no ownership | DoS/Grief | High |
| HIGH-08 | HIGH | Christmas world forces ADVENTURE, never restores gamemode | Logic Bug | High (2 independent audits) |
| HIGH-09 | HIGH | Krampus cage softlock when guards die without victim as killer | Logic Bug | High |
| HIGH-10 | HIGH | `/rename`: no permission, no length cap, full MiniMessage incl. click/hover injection | Injection | Verified |
| HIGH-11 | HIGH | `/sudo` blacklist misses permission plugins -> rank escalation | Privilege Escalation | High |
| HIGH-12 | HIGH | Black Friday discounts become permanent after crash | Economy | High |
| HIGH-13 | HIGH | DialogueManager leaks persistent TextDisplay entities + maps on disconnect | Resource Leak | High |
| HIGH-14 | HIGH | Flatfile storage mutates shared YamlConfiguration off-main thread | Race Condition | High |
| HIGH-15 | HIGH | Advent GUI treats clicks in player's own inventory as day claims | Logic Bug | High |
| MED-01..MED-29 | MEDIUM | see sections 7.x | various | various |
| LOW-01..LOW-27 | LOW | see section 8 | various | various |

---

## 4. CRITICAL findings

### CRIT-01 — Double-chest "lock migration" transfers ownership of the whole chest to whoever held a lock on one half
- **Location:** `src/main/java/com/smp/smptools/locks/LockManager.java:124-162` (inside `getBlockKey`)
- **Category:** Theft/Grief — **Confidence:** Verified (re-read directly)
- **Evidence:**
```java
if (!containerOwners.containsKey(doubleKey)) {
    if (containerOwners.containsKey(otherKey)) {
        UUID owner = containerOwners.remove(otherKey);
        ...
        containerOwners.put(doubleKey, owner);   // stale single-half lock becomes owner of BOTH halves
```
`getBlockKey()` canonicalizes a double chest to its lower-coordinate half and *migrates* any single-half lock onto the shared key. This runs on **read paths** (`isLocked`/`canAccess`), so the migration happens automatically.
- **Exploit scenario:** (1) Attacker places a chest at `(11,65,0)`, runs `/lock`. (2) Victim later places their own chest at `(10,65,0)`; Minecraft auto-pairs them into a double chest; victim stores valuables. (3) Next `getBlockKey()` call on either half (victim simply opening it, or a hopper transfer firing `InventoryMoveItemEvent`) promotes the attacker's lock to owner of `doubleKey = w:10:65:0` — the entire double chest. Attacker opens and drains everything; victim is locked out. No further attacker action required.
- **Impact:** Complete theft of victim storage; victim cannot even break their own half (`LockListener.onBlockBreak` denies via `canAccess`).
- **Suggested fix:** Never mutate ownership inside `getBlockKey`. On new pairing either drop the stale single-half lock or keep it scoped to the original half and notify both players.
- **Verification steps:** Read `LockManager.getBlockKey()` fully; confirm the migration branch executes on read paths by checking callers of `getBlockKey` (`isLocked`, `canAccess`, `LockListener.onPlayerInteract`). Dynamic test on a Paper 1.21 server: reproduce the 3-step scenario above.

### CRIT-02 — `lockContainer()` has no "already locked / not yours" guard
- **Location:** `src/main/java/com/smp/smptools/locks/LockManager.java:272-280`; only guard is pre-check in `src/main/java/com/smp/smptools/commands/LockCommand.java:59-63`
- **Category:** Theft/Grief — **Confidence:** High
- **Evidence:** `lockContainer()` does `containerOwners.put(key, owner.getUniqueId())` unconditionally; the command's `isLocked()` pre-check passes whenever the target is unlocked.
- **Exploit scenario:** Victim places an unlocked chest at `(10,65,0)`; attacker places adjacent `(11,65,0)` (auto-pairs), looks at their own half, runs `/lock`. `getBlockKey` returns the canonical key covering both halves -> attacker becomes owner of the victim's half too. Victim then cannot break their own half to recover items (deny path in `LockListener.java:50-62`). Any future non-command caller of `lockContainer` is also unprotected.
- **Impact:** Theft-by-extortion / permanent grief of storage.
- **Suggested fix:** Move ownership guard into `lockContainer` itself (refuse if key already owned unless same owner/admin). Also allow breaking your own chest half when the paired lock belongs to someone else.
- **Verification steps:** Read `lockContainer` and confirm no containsKey guard; read `LockCommand` to confirm the pre-check is the only protection; trace `getBlockKey` for paired chests.

### CRIT-03 — Image decompression bomb -> remote OOM crash of the server
- **Location:** `src/main/java/com/smp/smptools/imagemap/ImageProcessor.java:17-24`
- **Category:** DoS/Crash — **Confidence:** Verified (re-read directly)
- **Evidence:**
```java
BufferedImage downloadedImage = ImageIO.read(inputStream);   // allocates by DECODED pixel dimensions
...
BufferedImage resizedImage = resizeImage(downloadedImage, width, height); // + getScaledInstance + dither buffer
```
The download size cap (`BoundedInputStream`, ~10MB per `Constants.MAX_IMAGE_DOWNLOAD_BYTES`) limits compressed bytes only. A hand-crafted PNG of ~500KB-2MB can declare e.g. 30000x30000 pixels -> ~3.6 GB heap allocation instantly.
- **Exploit scenario:** Any player runs `/tomap <url-to-bomb-png>` (no permission node - see MED-08). Runs async so the main thread doesn't lag first; the JVM simply OOMs and dies.
- **Impact:** Any player can kill the server repeatedly.
- **Suggested fix:** Before decoding, read dimensions via `ImageIO.getImageReaders()` + `reader.getWidth()/getHeight()`; reject if `width*height > ~4096*4096`.
- **Verification steps:** Confirm no dimension check exists before `ImageIO.read`; confirm `/tomap` permission situation (see MED-08); dynamic test with a crafted large-dimension PNG against a small-heap test server.

### CRIT-04 — SSRF: DNS-rebinding TOCTOU bypasses URLValidator; blocked-range gaps
- **Location:** `src/main/java/com/smp/smptools/utils/URLValidator.java:84` (validation resolve) vs `:115` (connect re-resolve); range check at `:99-104`
- **Category:** SSRF/Network — **Confidence:** High
- **Evidence:**
```java
InetAddress resolved = InetAddress.getByName(host);          // resolution #1 (validation)
...
HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // resolution #2 (connect)
return address.isLoopbackAddress() || address.isAnyLocalAddress()
    || address.isLinkLocalAddress() || address.isSiteLocalAddress(); // misses fc00::/7, 100.64/10, etc.
```
Hostname resolved once for validation, then again at connect time. Attacker-controlled DNS with TTL 0 returns a public IP for check #1 and `127.0.0.1` / `169.254.169.254` / internal RFC1918 for connection #2. Redirect targets are re-validated but suffer the same race. Additionally `isSiteLocalAddress()` does not cover IPv6 unique-local `fc00::/7` (default Docker/K8s nets), CGNAT `100.64.0.0/10` (Tailscale), `198.18.0.0/15`, multicast.
- **Exploit scenario:** Player uses `/tomap http://evil.example/img.png` where evil.example DNS alternates answers; server fetches internal metadata/config endpoints; response errors surfaced to player (see LOW-08) enable internal port/host probing.
- **Impact:** Internal network access from a player-triggered primitive; affects both `/tomap` and `/music`.
- **Suggested fix:** Resolve once with `InetAddress.getAllByName()`, validate all addresses, connect to the pinned address (custom SocketFactory / raw socket with Host header). Explicitly reject `fc00::/7`, `100.64/10`, `198.18/15`, multicast.
- **Verification steps:** Re-read `URLValidator.validateAndCreate` end-to-end including redirect loop; confirm no pinning mechanism; check both call sites (`MapCommand`, `MusicCommand`).

---

## 5. HIGH findings

### HIGH-01 — Trade click/drag handlers lack `ignoreCancelled` -> item duplication via later cancellation
- **Location:** `src/main/java/com/smp/smptools/listeners/TradeListener.java:32` (`onInventoryClick`, `priority = HIGH`), `:145` (`onInventoryDrag`)
- **Category:** Item Duplication — **Confidence:** Verified
- **Evidence:** Both handlers run at `EventPriority.HIGH` with no `ignoreCancelled = true` and no `event.isCancelled()` check. The shift-click path manually mutates the trade inventory (`transferToTradeSlots`: `current.setAmount(...)` / `tradeInv.setItem(slot, newItem)`) and shrinks the source stack.
- **Exploit scenario:** Another plugin (WorldGuard region flags, protection plugins) cancels the same click at HIGHEST/MONITOR after this handler ran — or cancels at LOW before it. Vanilla rolls back the source slot transaction on cancellation, but the direct `setItem`/`setAmount` mutations to the trade inventory persist -> player keeps full stack AND copies appear in trade slots. Complete on both sides = duplication.
- **Impact:** Item dupe conditional on a co-existing protection plugin cancelling inventory clicks (common on SMP servers).
- **Suggested fix:** Add `ignoreCancelled = true` to both handlers; perform mutations only when the event is not cancelled; consider MONITOR+`hasChanged()` review.
- **Verification steps:** Confirm annotations; confirm `transferToTradeSlots` mutates live stacks; check whether any other registered listener in this plugin cancels InventoryClickEvent after HIGH.

### HIGH-02 — Trade GUI remains interactive for 1 tick after completion/cancel; GUI decoration theft
- **Location:** `src/main/java/com/smp/smptools/trade/TradeSession.java:190-212` (`scheduleCloseInventories` via `runTask`), `src/main/java/com/smp/smptools/listeners/TradeListener.java:36-37`
- **Category:** Logic Bug — **Confidence:** Verified
- **Evidence:** `completeTrade()`/`cancelTrade()` call `removeSession(...)` then close inventories next tick. In that window `tradeManager.getSession(player)` returns null so the click handler early-returns WITHOUT cancelling -> all clicks in the still-open top inventory are vanilla-processed. Top inventory still contains barrier/panes/ready buttons.
- **Exploit scenario:** Player times a click on the CANCEL barrier (slot 49) or ready panes right as the other side completes -> picks up GUI decoration items onto cursor (returned to them on close). Minor theft/sloppiness; also demonstrates the session-null gap.
- **Impact:** Low-value item theft; indicates missing post-completion guard.
- **Suggested fix:** Keep session resolvable until after close, or cancel all clicks while view title matches and session recently ended; clear button items synchronously.
- **Verification steps:** Trace ordering of `removeSession` vs scheduled close; confirm no cancellation guard for session-less clicks.

### HIGH-03 — Graves orphaned on head break/explosion; anyone can loot by replacing the head
- **Location:** `src/main/java/com/smp/smptools/graves/GraveManager.java:124-146` (interact-only looting), `:194-219` (`lootGrave`); NO BlockBreakEvent/EntityExplodeEvent handling anywhere in file
- **Category:** Theft/Grief — **Confidence:** Verified
- **Evidence:** Graves are PLAYER_HEAD blocks tracked only in the `graves` map + graves.yml. Only `PlayerInteractEvent` triggers looting. Breaking the head drops just the head item; the grave entry stays in the map/yml forever.
- **Exploit scenario:** (a) Griefer breaks victim's grave head (head stolen), later places ANY player head at the same Location, right-clicks -> `graves.containsKey(location)` is true -> entire dead player's inventory drops for the taking. (b) Explosions destroy the head -> permanent orphaned entries accumulating in graves.yml.
- **Impact:** Grave theft bypassing ownership; unbounded yml growth; inconsistent state.
- **Suggested fix:** Handle BlockBreakEvent/explosions for grave blocks (cancel or finalize/remove grave); bind graves to block entity PDC instead of bare location; add expiry cleanup.
- **Verification steps:** Grep GraveManager for BlockBreak/Explode handlers (none); verify interact path does not check looter identity (README implies public looting - flag design intent vs break/place hole).

### HIGH-04 — Grave keyed by Location: second death at same spot silently destroys first grave's items
- **Location:** `src/main/java/com/smp/smptools/graves/GraveManager.java:117` (`graves.put(validLocation, grave)`)
- **Category:** Item Loss — **Confidence:** Verified
- **Evidence:** Map key is `Location`; a new Grave object overwrites the previous value with no merge/move logic. `createGraveBlock` replaces the head block too.
- **Exploit/Failure scenario:** Player dies twice in the same block (common: same base/spawn) -> first grave's ItemStack list becomes unreferenced = items gone permanently; holograms of first grave also orphaned.
- **Impact:** Silent irreversible item loss.
- **Suggested fix:** On collision, offset second grave to nearest free valid location (the scan already exists) or merge item lists into one grave.
- **Verification steps:** Confirm no containsKey check before put; trace death flow lines 62-122.

### HIGH-05 — Private vault decode failure permanently destroys contents; unbounded `readInt()` OOM
- **Location:** `src/main/java/com/smp/smptools/commands/PrivateVaultCommand.java:42-50` (decode catch leaves empty vault), `:76` (`readInt()` unbounded); `src/main/java/com/smp/smptools/listeners/VaultListener.java:31-35` (close always overwrites blob)
- **Category:** Item Loss / DoS — **Confidence:** High
- **Evidence:** Vaults are Java-native-serialized Base64 blobs in config.yml. On `IOException | ClassNotFoundException` the vault opens EMPTY with only a console warning; the first `InventoryCloseEvent` then serializes the empty inventory over the original blob. `readInt()` sizes `new ItemStack[n]` from the stream with no bound.
- **Failure/Exploit scenario:** Minecraft version upgrade changes serialization format -> every affected player's vault wiped on first open+close. Tampered/hand-edited blob with huge int -> OOM on open.
- **Impact:** Irreversible loss of all vault contents per affected player; potential memory DoS.
- **Suggested fix:** On decode failure refuse to open AND refuse close-save (keep old blob, notify admin); clamp `readInt()` to configured vault size; migrate to YAML ConfigurationSerializable storage like artifacts/bounties use.
- **Verification steps:** Read decode method + VaultListener close path; confirm overwrite happens even when inventory empty; check any size validation on readInt.

### HIGH-06 — Chunk-loader listeners ignore event cancellation -> phantom immortal loaders + beacon duplication
- **Location:** `src/main/java/com/smp/smptools/listeners/ChunkLoaderListener.java:24-54`
- **Category:** Item Duplication / DoS — **Confidence:** High
- **Evidence:** `onBlockPlace`/`onBlockBreak` run at NORMAL priority, no `ignoreCancelled`, no isCancelled check. Break path calls `removeChunkLoader(...)` + `setDropItems(false)` + drops a fresh loader item.
- **Exploit scenario:** With WorldGuard-style plugins that cancel AFTER NORMAL priority: place -> loader registered for a block that was never placed (invisible, unbreakable, permanently force-loaded chunk; player keeps item). Break -> loader deregistered AND new beacon dropped although block survives -> repeatable beacon-item duplication.
- **Impact:** Unremovable lag machines; free chunk-loader beacons.
- **Suggested fix:** `priority = MONITOR, ignoreCancelled = true` for state mutation; drop item only on final break.
- **Verification steps:** Read listener annotations; grep project for other BlockPlace/BlockBreak cancellers and their priorities; dynamic test with a dummy cancelling plugin.

### HIGH-07 — Chunk loaders: no cap, no explosion/wither handling, no ownership
- **Location:** `src/main/java/com/smp/smptools/chunkloaders/ChunkLoaderManager.java:27` (plain ArrayList, no owner field), `:108-121` (add/remove); no EntityExplodeEvent/BlockExplodeEvent/EntityChangeBlockEvent handlers for loaders anywhere (grep)
- **Category:** DoS/Grief — **Confidence:** High
- **Evidence/Scenario:** Unlimited placement -> unbounded force-loaded chunks. TNT destroys beacon (blast resistance 3) without deregistration -> entry lives forever in yml, chunk stays loaded, dropped beacon re-placed elsewhere -> monotonic growth. No owner UUID -> anyone steals others' loaders; quotas impossible.
- **Impact:** Server lag weaponization; item farming loop.
- **Suggested fix:** Per-player/server caps; store owner; handle explosions/wither by removing affected positions or protecting blocks; admin list/cleanup subcommand.
- **Verification steps:** Confirm absence of cap logic in addChunkLoader; grep for explode handlers referencing ChunkLoaderManager.

### HIGH-08 — Christmas world forces ADVENTURE gamemode, never restores it
- **Location:** `src/main/java/com/smp/smptools/listeners/ChristmasWorldListener.java:40-57`
- **Category:** Logic Bug — **Confidence:** High (found independently by two audits)
- **Evidence:** `PlayerChangedWorldEvent` into `christmas` -> `setGameMode(GameMode.ADVENTURE)`. Grep confirms these are the ONLY setGameMode calls; `PortalListener` returns players to overworld without restoring; no exit hook, no death/respawn restore.
- **Failure scenario:** Any survival player visits once -> stuck in ADVENTURE in overworld (cannot break/place/fight) until admin intervention. Weaponizable against new players.
- **Impact:** Account-bricking gameplay bug generating tickets.
- **Suggested fix:** Store previous gamemode (map/PDC) on entry; restore on leave/quit/death; or enforce christmas restrictions via region checks instead of gamemode mutation.
- **Verification steps:** Grep all `setGameMode` occurrences; read PortalListener return path; confirm no restore hook.

### HIGH-09 — Krampus cage softlock when guards die without the victim as killer
- **Location:** `src/main/java/com/smp/smptools/listeners/KrampusListener.java:100-109`; `src/main/java/com/smp/smptools/christmas/KrampusManager.java:292-305` (`checkGuardDeath` only removes from set; release requires `guards.isEmpty()`)
- **Category:** Logic Bug — **Confidence:** High
- **Evidence:** Release fires only when the kidnapped player lands the killing blow on EVERY guard. Guard death with null/foreign killer (admin `/kill`, ClearLagg-style purges, environment) never removes its UUID from `playerGuards`.
- **Failure scenario:** Victim kills all live guards but one guard was entity-cleared earlier -> `guards.isEmpty()` never true -> imprisoned in sky cage indefinitely (only relog escapes via quit handler).
- **Impact:** Permanent imprisonment softlock; guaranteed occurrence on servers with entity-clearing plugins.
- **Suggested fix:** Track liveness (EntityDeathEvent regardless of killer + periodic `Bukkit.getEntity(id) == null` validation) rather than killer identity.
- **Verification steps:** Read guard-death handler conditionals; confirm no other removal path from the guards set.

### HIGH-10 — `/rename`: no permission node, no length cap, full MiniMessage incl. `<click:run_command>`
- **Location:** `src/main/java/com/smp/smptools/commands/RenameCommand.java:41-45`; `src/main/resources/plugin.yml:63-65` (no permission key); sanitizer exists unused: `utils/InputValidator.sanitizeMiniMessage()` (~lines 72-91)
- **Category:** Injection — **Confidence:** Verified (re-read directly)
- **Evidence:** `MiniMessage.miniMessage().deserialize(rawName)` applied to held item with zero gating/validation.
- **Exploit scenario:** Any player creates an item whose display name carries `<click:run_command:'/pay Attacker 10000'>` + hover spoofing. Vanilla death messages include the weapon name component -> victims clicking "slain by [Weapon]" execute the command AS THEMSELVES. Even without funny-death-messages, hover-spoofed tooltips spread via trades/chests/dropped items. No length limit -> multi-KB tooltip lag.
- **Impact:** Click-jacking command execution against other players; chat/UI abuse.
- **Suggested fix:** Gate behind permission; run through `sanitizeMiniMessage()`; strip click/hover/insertion/font; cap length ~30-64 chars.
- **Verification steps:** Confirm absence of permission check in command + plugin.yml; confirm death-message path passes item components untouched (`DeathMessageListener.java:36-52`).

### HIGH-11 — `/sudo` blacklist misses permission-plugin commands -> rank escalation
- **Location:** `src/main/java/com/smp/smptools/commands/SudoCommand.java:42-52`; `src/main/java/com/smp/smptools/utils/CommandBlacklist.java:18-23`
- **Category:** Privilege Escalation — **Confidence:** High
- **Evidence:** Runs `target.performCommand(stripped)` (as target, good) and `target.chat(...)`. Blacklist covers vanilla/Bukkit commands only.
- **Exploit scenario:** Admin holding `smptools.sudo` socially engineered into `/sudo SomeAdmin lp user Attacker parent add group.admin` executes LuckPerms promotion AS that admin. No equal/higher-privilege target protection either.
- **Impact:** Full rank escalation via one gullible admin action.
- **Suggested fix:** Allowlist safe commands or typed confirmation; refuse targets holding sudo/op without second confirmation; add `permission:` key in plugin.yml.
- **Verification steps:** Read CommandBlacklist contents fully; confirm no LP/PEX entries; confirm dispatch identity is target not console.

### HIGH-12 — Black Friday discounts become permanent across crash/restart
- **Location:** `src/main/java/com/smp/smptools/listeners/BlackFridayListener.java:26` (`originalRecipes` in-memory ConcurrentHashMap), `:56-64`
- **Category:** Economy — **Confidence:** High
- **Evidence:** Original MerchantRecipes kept only in memory; villagers persist their (discounted) recipes to disk. Crash mid-event -> on reboot nothing restores originals.
- **Failure scenario:** Every villager trade stays at up to 90% off forever until manual repair -> economy leak.
- **Suggested fix:** Persist discounted-villager UUIDs/originals to disk on apply; restore on startup; or re-derive originals by inverse transform.
- **Verification steps:** Confirm no persistence of originals; check startup restore path existence.

### HIGH-13 — DialogueManager leaks persistent TextDisplay entities + player maps on disconnect
- **Location:** `src/main/java/com/smp/smptools/managers/DialogueManager.java:126-142` (watchdog `!player.isOnline()` branch cancels WITHOUT `removeTextDisplay`); maps at `:31-34` never cleaned on quit (no PlayerQuitEvent handler touches DialogueManager)
- **Category:** Resource Leak — **Confidence:** High
- **Evidence:** Spawned TextDisplay is a real persistent entity (`setVisibleByDefault(false)` only hides rendering).
- **Failure scenario:** Repeat join/NPC-click/quit cycles accumulate invisible entities forever -> chunk bloat, entity tick lag, manual `/kill` cleanup required.
- **Suggested fix:** Quit handler calling `stopDialogue(player)`; watchdog offline branch must remove display before cancel.
- **Verification steps:** Read watchdog task; grep for PlayerQuitEvent handlers calling dialogue cleanup; confirm TextDisplay persistence semantics.

### HIGH-14 — Flatfile storage mutates shared YamlConfiguration off the main thread
- **Location:** `src/main/java/com/smp/smptools/tags/TagManager.java:87` (async dispatch) -> `:100` (`getAllPlayerTitles`) and `:109` (`savePlayerTitle`); `src/main/java/com/smp/smptools/storage/FlatFileStorageProvider.java:95-98,112-121` (reads/sets `tagsConfig` off-main)
- **Category:** Race Condition / Data Corruption — **Confidence:** High
- **Evidence:** For FLATFILE provider, title load/save runs on a Bukkit async thread while main thread mutates the same non-thread-safe YamlConfiguration (backed by LinkedHashMap) via `unlockTitle()` (`TagManager.java:438-445`) etc. Note `snapshotLegacyTitlesSync()` (:126-138) correctly hops to main thread - the primary path doesn't.
- **Failure scenario:** ConcurrentModificationException, corrupted in-memory config, truncated/garbled tags.yml snapshot.
- **Impact:** Data corruption/loss for titles in flatfile mode.
- **Suggested fix:** Route flatfile config access through main thread (as snapshotLegacyTitlesSync does) or single-threaded owner executor; cache titles in ConcurrentHashMap and never touch YAML off-main.
- **Verification steps:** Trace thread of each call site; inspect FlatFileStorageProvider methods for direct config access.

### HIGH-15 — Advent GUI treats clicks in the player's OWN inventory as day claims
- **Location:** `src/main/java/com/smp/smptools/listeners/AdventGUIListener.java:89-101`
- **Category:** Logic Bug — **Confidence:** High
- **Evidence:** Handler cancels everything then uses `event.getSlot()` (index within CLICKED inventory) as day number. Clicking your own hotbar slot 0-24 while calendar open yields day=1-25, passes `day <= currentDay`, runs full claim path incl. console reward commands.
- **Exploit scenario:** Click dirt in hotbar slot 5 on Dec 10 -> silently claims day 6 reward. Also blocks all normal inventory management while GUI open.
- **Impact:** Unintended reward trigger; proves missing top-inventory verification.
- **Suggested fix:** Early-return unless `getClickedInventory() == getView().getTopInventory()` (or rawSlot < size); derive day from item PDC not slot math.
- **Verification steps:** Read handler; confirm no clicked-inventory check; trace claim path side effects.

---

## 6./7. MEDIUM findings

### MED-01 — Non-atomic config file writes (truncate-in-place) across the persistence layer
- **Location:** `utils/AsyncConfigHelper.java:104,116` (`Files.write` truncates live file); same pattern via `FileConfiguration.save()` in `BountyManager.java:208`, `MissionManager.java:111`, `SeasonalManager.java:153,164`, `ArtifactManager.java:176`, `VaultListener.java:34`, `GraveManager.saveGraves`, `EventManager.saveData`
- **Category:** Data Loss/Corruption — **Confidence:** High
- **Scenario:** Crash/OOM/kill mid-write leaves stats.yml (ALL player stats in flatfile), tags.yml, bounties.yml etc. truncated -> full dataset silently reset next boot.
- **Fix:** Write to `file.tmp` + `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`; optional fsync.
- **Verify:** Confirm no temp+rename anywhere; check AsyncConfigHelper write calls.

### MED-02 — MySQL/MariaDB defaults to `useSSL=false`
- **Location:** `storage/JdbcStorageProvider.java:52-57`
- **Category:** Data Exposure — **Confidence:** High
- **Scenario:** DB credentials and player data sent plaintext by default.
- **Fix:** Default `useSSL=true` / `sslMode=VERIFY_CA`; explicit opt-out only.
- **Verify:** Read URL construction + config default.

### MED-03 — Read-after-write staleness: async write queue vs synchronous reads
- **Location:** `storage/JdbcStorageProvider.java:252-273` (queued writes) vs `:276-293` (direct reads); same in `MongoStorageProvider.java:202-234`
- **Category:** Race Condition — **Confidence:** High
- **Scenario:** `saveStat` returns before commit; immediate read on another thread sees old value (e.g., trail set then fast rejoin restores previous trail).
- **Fix:** Document eventual consistency; add pending-write cache for reads; or return futures.
- **Verify:** Trace save/get paths and threading.

### MED-04 — Every vault close serializes ENTIRE config.yml synchronously on main thread
- **Location:** `listeners/VaultListener.java:32-34`
- **Category:** Performance / Data Exposure — **Confidence:** High
- **Scenario:** O(all-vaults) main-thread stall per vault close as blobs accumulate; widens corruption window of the credentials-containing file (compounds MED-01).
- **Fix:** Move vaults to own file/storage provider with async atomic saves.
- **Verify:** Confirm `plugin.saveConfig()` call path.

### MED-05 — Blocking DNS resolution on the main thread during command validation
- **Location:** `music/MusicCommand.java:92-98`, `imagemap/MapCommand.java:37-43` (sync `URLValidator.validateAndCreate` inside command executor)
- **Category:** DoS/Performance — **Confidence:** High
- **Scenario:** Player spams `/tomap http://<random-host>/` -> resolver timeouts (~5s x retries) stall ticks repeatedly.
- **Fix:** Move validation into the async task or use short-timeout resolve off-main.
- **Verify:** Confirm validateAndCreate performs getByName synchronously and is called pre-async.

### MED-06 — Malformed NBS note key crashes playback task every tick
- **Location:** `music/NBSParser.java:78-79` (no range check); `music/SongPlayer.java:37` (`new Note(key - 33)` throws unless 0..24)
- **Category:** DoS/Crash — **Confidence:** High
- **Scenario:** Hostile `.nbs` (any URL, no permission) with key=0 -> IllegalArgumentException on MAIN thread every repeating tick for song duration (up to 65534 ticks): console spam, broken audio.
- **Fix:** Clamp/reject notes outside 33..57; validate instrument range.
- **Verify:** Read parser fields vs Note constructor contract.

### MED-07 — Lumberjack enchant: unbounded recursive flood fill -> StackOverflow / mass-break lag
- **Location:** `listeners/EnchantmentListener.java:94-111` (`collectLogs` recursion, 26 neighbors, no cap)
- **Category:** DoS/Crash — **Confidence:** High
- **Scenario:** Player builds serpentine 50k-log structure, breaks one block with lumberjack tool -> StackOverflowError on main thread or thousands of breaks in one tick. (Contrast: artifact Timber Axe caps at 64 - `ArtifactListener.fellTree`.)
- **Fix:** Cap collected size (~512); iterative queue.
- **Verify:** Confirm absence of cap; compare with ArtifactListener implementation.

### MED-08 — `/tomap` and `/music play` have no permission node at all
- **Location:** `plugin.yml:73-78`; `MusicCommand.java:38-45` (only broadcast gated)
- **Category:** Abuse surface — **Confidence:** High
- **Scenario:** Any player: unlimited outbound HTTP GETs (proxy/bandwidth abuse, internal probing within validator limits), world-wide sounds without cooldown, up to 64 maps per invocation persisted to imagemaps.yml and re-downloaded EVERY restart (`MapManager.java:24-57`) -> storage growth + startup fetch storm.
- **Fix:** Add permission keys; per-player rate limit; cap stored maps.
- **Verify:** plugin.yml sections; MapManager restart reload loop.

### MED-09 — TrollGUI identified by inventory title string (forgeable context)
- **Location:** `listeners/TrollGUIListener.java:167-176` (title prefix match), `:183` (target parsed from title)
- **Category:** Logic Bug/Abuse — **Confidence:** High
- **Scenario:** Anvil-named chest "Troll Menu - Victim" gets its clicks cancelled (nuisance); if clicked items carry matching CustomModelData (circulate once created via `/customitem`), troll fires against named player with NO permission re-check in listener.
- **Fix:** Identify by InventoryHolder type (pattern exists: `InvseeGUIListener.java:91`); re-check permission in handler.
- **Verify:** Read identification logic; confirm no permission check inside click branch.

### MED-10 — Christmas blanket damage cancel includes VOID -> infinite fall softlock
- **Location:** `listeners/ChristmasWorldListener.java:59-66`
- **Category:** Logic Bug — **Confidence:** High
- **Scenario:** Cancelling VOID damage prevents death: player falls forever, unkillable even by `/kill` (cancellable damage). Also grants total combat immunity in that world.
- **Fix:** `if (cause != VOID) cancel`; teleport back above minHeight.
- **Verify:** Read handler; confirm no cause filter.

### MED-11 — Lock removal/migration mutates state at MONITOR priority
- **Location:** `listeners/LockListener.java:64-82` (`onBlockBreakConfirm` at MONITOR, ignoreCancelled=true)
- **Category:** Protection Bypass — **Confidence:** High
- **Scenario:** Another plugin cancelling the same event at MONITOR after this one leaves chest intact but lock deleted/migrated -> publicly accessible container.
- **Fix:** Mutate in HIGH handler after access check, or metadata-marker + commit pattern.
- **Verify:** Priorities of both break handlers; ordering semantics.

### MED-12 — Ender chests are lockable -> unbreakable grief of public infrastructure
- **Location:** `locks/LockManager.java:238-251` (`type.name().contains("CHEST")` matches ENDER_CHEST)
- **Category:** Grief — **Confidence:** High
- **Scenario:** Anyone `/lock`s spawn ender chest: interact blocked for everyone (per-player inventory so owner gains nothing), break blocked for non-admins -> permanent grief needing admin cleanup.
- **Fix:** Exclude ENDER_CHEST in isContainer.
- **Verify:** Material matching logic.

### MED-13 — Wither (EntityChangeBlockEvent) bypasses lock explosion protection
- **Location:** `listeners/LockListener.java:106-114` covers only EntityExplodeEvent/BlockExplodeEvent; grep shows no EntityChangeBlockEvent handler in project
- **Category:** Protection Bypass — **Confidence:** High
- **Scenario:** Wither eats locked chests via block-change events (contents drop, lock orphaned).
- **Fix:** Handle EntityChangeBlockEvent for locked containers.
- **Verify:** Grep event types in LockListener.

### MED-14 — Teleport destinations never safety-checked (suffocation/void)
- **Location:** `commands/HomeCommand.java:57-58`; `tpa/TpaManager.java:126`; `teleport/TeleportManager.java:52-53`
- **Category:** Logic Bug — **Confidence:** High
- **Scenario:** Sealed home -> suffocation loop; homes near build edges -> void drops. No block scan/world-border/Y-bound checks.
- **Fix:** Safe-spot search (feet/head passable + solid floor) before final teleport; abort with message otherwise.
- **Verify:** Read startTeleport + home load path for any safety logic.

### MED-15 — Riding bypasses teleport warmup movement-cancel
- **Location:** `teleport/TeleportListener.java:24-43` (block-change check only), `TeleportManager.java:37-62`
- **Category:** Logic Bug — **Confidence:** High
- **Scenario:** PlayerMoveEvent doesn't fire while passenger: start `/home`, hop into boat/minecart, be carried hundreds of blocks during countdown; teleport completes from new location.
- **Fix:** Also listen VehicleMoveEvent/EntityDismountEvent; cancel on vehicle enter.
- **Verify:** Handler list for movement sources.

### MED-16 — TPA accept does not validate requester liveness; death doesn't cancel warmup
- **Location:** `tpa/TpaManager.java:112-129` (`isOnline()` true for dead players); no PlayerDeathEvent hook cancels pending teleports (grep)
- **Category:** Logic Bug — **Confidence:** High
- **Scenario:** Accept after sender died -> corpse-state player teleported; undefined behavior.
- **Fix:** Check `!requester.isDead()`; death handler cancels pending teleports both parties.
- **Verify:** Accept path checks; grep death handlers re teleports.

### MED-17 — AFK players force-counted as YES sleep votes; cross-world vote counting
- **Location:** `sleep/SleepManager.java:46-52` (AFK auto-added to yesVotes), `:69-108` (addVote accepts any world; checkVoteStatus counts initiator-world players only)
- **Category:** Logic Bug — **Confidence:** High
- **Scenario:** With auto-vote-sleep default, 1 awake player + >=50% AFK server skips night with zero consent; cross-world votes skew thresholds; quit votes retained.
- **Fix:** Exclude other worlds; drop offline votes; make AFK auto-yes configurable/opt-in.
- **Verify:** Read both methods fully.

### MED-18 — SitListener spawns duplicate leaked marker armor stands
- **Location:** `listeners/SitListener.java:33-79` (no `getHand()` check, no ignoreCancelled)
- **Category:** Resource Leak — **Confidence:** High
- **Scenario:** PlayerInteractEvent fires per hand; empty hands pass AIR check twice -> TWO marker stands per stair click; only dismounted-from one removed; markers accumulate until restart. Sits also occur when protection plugins cancel interact.
- **Fix:** `ignoreCancelled=true` + `if (event.getHand() != EquipmentSlot.HAND) return;` (pattern used in NPCListener.java:23).
- **Verify:** Read handler signature/body; entity cleanup path.

### MED-19 — `/fly` grant persists across death, world change, relog
- **Location:** `commands/FlyCommand.java:20-28` (`setAllowFlight(true)` maps to persisted abilities NBT); no death/world-change guards anywhere (grep)
- **Category:** Privilege persistence — **Confidence:** High
- **Scenario:** Flight survives respawn/dimension change/relog until manually toggled; toggling also strips creative flight unintentionally.
- **Fix:** Policy hooks (revoke on death/world change if desired); guard creative/spectator.
- **Verify:** Grep setAllowFlight occurrences + related listeners.

### MED-20 — NPC skin applied via raw console `data merge` string -> NBT injection point
- **Location:** `managers/NPCManager.java:156-159`; skin from npcs.yml OR `/npc spawn ... [skin]` args (`NPCCommand.java:37`, perm `smptools.npc.admin`)
- **Category:** Injection — **Confidence:** High
- **Scenario:** Crafted skin value like `Steve"},Invulnerable:0b,...` injects arbitrary NBT into console-executed data merge (vanilla lacks `;` chaining, limiting blast radius). Unvalidated `id` also used as YAML key (dots create nested sections, breaking removal).
- **Fix:** Strict `[A-Za-z0-9_]{1,16}` validation; avoid string-built commands (use API/PDC).
- **Verify:** Read command construction; check id validation in createNPC.

### MED-21 — Blocking Mojang profile lookups on main thread at startup/spawn
- **Location:** `managers/NPCManager.java:147-155`, `:259-260` (`profile.complete(true)` = sync HTTP)
- **Category:** Performance — **Confidence:** High
- **Scenario:** Slow/unreachable Mojang API stalls startup and `/npc spawn` for full timeout per NPC.
- **Fix:** Fetch skins async, apply on main tick.
- **Verify:** Call sites/thread context.

### MED-22 — Snowball Warfare has no world/date restriction + freeze-stacking CC
- **Location:** `listeners/SnowballListener.java:36-57`
- **Category:** Design/Balance — **Confidence:** High
- **Scenario:** Active globally whenever christmas feature enabled: snowballs deal config damage (x2 headshot, one-sided Y check fires often) to ANY LivingEntity incl. villagers/pets/bosses; 4 rapid hits hit 140-tick freeze cap = near-uncounterable spammable CC, all year, all worlds.
- **Fix:** Gate by world/date; exclude tamed/villagers; proper headshot band.
- **Verify:** Read handler gating conditions.

### MED-23 — Secret Santa: match regeneration destroys deposited gifts; leavers strand targets
- **Location:** `christmas/SecretSantaManager.java:98-123`; `commands/SecretSantaCommand.java:117-120`
- **Category:** Item Loss — **Confidence:** High
- **Scenario:** `/secretsanta admin start` overwrites matches unconditionally mid-PREPARATION: gifts keyed to old target UUIDs unreachable; new bijection can double-assign a recipient whose `hasGiftDeposited` then passes -> second santa's deposit silently overwrites first santa's items, no refund. Post-match leavers never reassigned -> their target gets nothing.
- **Fix:** Refuse start when gifts.* exist (or migrate); regenerate around offline participants.
- **Verify:** Read start/regeneration flow + gift keying.

### MED-24 — Krampus kidnap leaves orphaned cages / stale state on non-standard exits
- **Location:** `christmas/KrampusManager.java:185-224,307-329`; `listeners/KrampusListener.java:70-75`
- **Category:** Logic Bug — **Confidence:** High
- **Scenario:** cleanupStaleCages only handles post-restart orphans; chorus-fruit escape leaves kidnapped=true + live sky cage forever (also reserves airspace via isCageOverlapping); victim death->respawn far away softlocks cage; legit release force-teleports player to pre-kidnap location even if they moved since (position hijack).
- **Fix:** Periodic validation task (guards==0 or distance -> release); block consumables/pearls while kidnapped; optional release teleport.
- **Verify:** Read escape paths + cleanup triggers.

### MED-25 — Nature's Touch Hoe 5x5 harvest fires no BlockBreakEvent -> bypasses protection
- **Location:** `artifacts/ArtifactListener.java:225-245` (`crop.breakNaturally(main)` directly)
- **Category:** Protection Bypass — **Confidence:** Verified
- **Scenario:** Harvests/replants 25 crops per right-click without calling BlockBreakEvent: WorldGuard-protected crops broken, mission/skill trackers bypassed. (Contrast: Timber Axe correctly fires sub-events.)
- **Fix:** Fire BlockBreakEvent per crop and respect cancellation (mirror fellTree pattern).
- **Verify:** Compare with `fellTree` implementation in same file.

### MED-26 — Event live reward delivery failure = reward silently lost (not queued)
- **Location:** `events/minievents/MiniEventSession.java:272-318` (`deliverRewards`: executeReward false -> log warning only, offlineQueue only for offline players)
- **Category:** Item Loss — **Confidence:** Verified
- **Scenario:** Online winner whose reward command fails (economy plugin hiccup) never receives it; no retry/queue.
- **Fix:** On failure enqueue to offline/pending queue (reuse EventManager retry mechanism).
- **Verify:** Read deliverRewards branches.

### MED-27 — Event sidebar hijacks/conflicts with other plugins' scoreboards
- **Location:** `events/minievents/MiniEventSession.java:165-208` (`renderSidebar`)
- **Category:** Logic Bug — **Confidence:** Verified
- **Scenario:** Only replaces board when it equals main scoreboard; custom boards (teams plugins) get `smpevent` objective registered INTO them; restore-on-end handles smpevent unregister but team/sidebar slot conflicts possible; originalScoreboards map leaks for quitters until end().
- **Fix:** Track replaced boards explicitly; clean on quit; coordinate DisplaySlot ownership.
- **Verify:** Read renderSidebar + end() restore logic.

### MED-28 — Mob Frenzy scores spawner/grinder mobs (no SpawnReason filter)
- **Location:** `events/minievents/MiniEventListener.java:232-311`
- **Category:** Design/Balance — **Confidence:** Verified
- **Scenario:** AFK grinder farms dominate leaderboard; combo multiplier amplifies. Similar inherent AFK-fish concern for Fishing Derby.
- **Fix:** Optional SpawnReason.NATURAL/SPAWNER config weighting; document intent.
- **Verify:** Confirm no SpawnReason usage.

### MED-29 — Bounty claim crash window allows re-claim duplication
- **Location:** `bounty/BountyManager.java:322-352` (`claimOrRefundBounty`: memory mutations then ASYNC `saveBounties()`; claimed-bounty omitted from file writes entirely at `:184-186`)
- **Category:** Item Duplication (crash-only) — **Confidence:** Verified
- **Scenario:** Claim marks bounty claimed + removes it + queues async save; hard crash before flush -> old file still lists unclaimed bounty -> claim again after restart (items also delivered pre-crash). Single-file write makes normal operation atomic-ish; window is crash-only.
- **Fix:** Synchronous save (or fsync tmp+rename) on claim path; keep tombstone records until refund delivered.
- **Verify:** Read claim flow + writeBountiesToFile omission logic + shutdown flush ordering.

---

## 8. LOW findings (compact)

| ID | Location | Issue | Fix direction | Conf. |
|----|----------|-------|---------------|-------|
| LOW-01 | `utils/AsyncConfigHelper.java:31,37-48` | `shuttingDown` flag dead; executor resurrected after shutdown -> orphaned post-disable writes; `getExecutor()` holds monitor during 10s drain | Reject saves once shutting down; don't hold lock in awaitTermination | High |
| LOW-02 | `trails/TrailManager.java:87-89`; `tags/TagManager.java:134,186-188` | Unbounded `.get()` on `callSyncMethod` futures - async thread blocks forever if main thread stalls | Timed get(N, SECONDS) | High |
| LOW-03 | `listeners/TrailsGUIListener.java:32`; `trails/TrailManager.java:98` | Bukkit Player API (`isOnline`) touched from async threads | Capture via sync check / UUID-guarded pattern | High |
| LOW-04 | `leaderboard/LeaderboardManager.java:26` vs `:116`,`:40` | `lastCacheTime` plain long written async/read anywhere -> stale refresh | volatile | High |
| LOW-05 | `storage/MongoStorageProvider.java:226,395` vs encodeKey `:77-80` | Legacy `_`-key fallback visible to getStat but invisible to getAllPlayerStats -> milestone cache misses | Align fallbacks or migrate | High |
| LOW-06 | `SMPTools.java:340-352`; `JoinLeaveListener.java:33-35` | Stats persisted only every 5 min; quit updates memory only -> crash loses up to 5 min of stats | Save on quit or accept tradeoff | High |
| LOW-07 | `commands/CustomItemCommand.java:31,48-49` | `Material.valueOf("AIR")` accepted -> null meta NPE | isItem()/null-meta guard | High |
| LOW-08 | `imagemap/MapCommand.java:121-124` | Raw `e.getMessage()` sent to player -> internal error info disclosure for probing | Generic error to player, log details | High |
| LOW-09 | `music/MusicCommand.java:22,136` | Song keeps playing to world after player quits; playingTasks entry not cleaned on quit | Quit hook cancels task | High |
| LOW-10 | `commands/SoundCommand.java:35-37` | No cooldown on world-wide meme sounds (permission-gated) | Per-player cooldown | High |
| LOW-11 | `chat/ChatManager.java:67-69`; `NameTagListener.java:47` | tagTitle interpolated raw into MiniMessage hover template (currently config-derived trusted path - fragile) | Sanitize like prefix/color | High |
| LOW-12 | `chunkloaders/ChunkLoaderManager.java:228` | Loader item detection falls back to displayName+lore equality -> anvil forgery if admin configures plain name/empty lore | PDC-only detection | High |
| LOW-13 | `listeners/PortalListener.java:39-63`, hardcodes `Bukkit.getWorlds().get(0)` at `:53` | Portal checks run every move tick without block-change short-circuit; no vehicle/dead guards; world-order assumption breaks return teleport | Block-change guard; resolve overworld by environment | High |
| LOW-14 | `listeners/LockListener.java:84-104` | Locked hoppers/dispensers fully sealed even for owner (InventoryMoveItemEvent/BlockDispenseEvent cancelled unconditionally) | Owner passthrough option / document | High |
| LOW-15 | `locks/LockManager.java:238-251` | CRAFTER, LECTERN, DECORATED_POT not lockable while hoppers can drain them (1.21 gap) | Extend isContainer list | High |
| LOW-16 | `accelerators/CropAccelerator.java:27,53-64,84-87` | Bone-meal no-op for cane/cactus/bamboo (silently never accelerate); up to multiplier x loaded-chunks height scans per second on main thread | Manual growth logic; cap chunks/tick | High |
| LOW-17 | `chat/ChatManager.java:17` | `lastMessengers` never cleaned on quit (unbounded growth) | Quit cleanup | High |
| LOW-18 | `tpa/TpaManager.java:37-39,64-68` | tpaToggledOff persists across relogs (surprising + set growth); single request slot per target lets anyone block senders 60s | Clear on quit; key by (target,requester) | High |
| LOW-19 | `commands/DailyRewardCommand.java:35` | Uncaught DateTimeParseException on malformed stored timestamp -> /daily exceptions until manual fix | try/catch treat as never-claimed | High |
| LOW-20 | `listeners/AdventGUIListener.java:85` | GUI identified by exact title Component string - other plugins' views with same title hijacked | InventoryHolder pattern | High |
| LOW-21 | `listeners/FestiveMobsListener.java:134-141` | Candy cane: ClassCastException on non-flat display components aborts handler inconsistently; no hand check consumes two; anvil-renamed sugar works as cheap Speed II | Robust component handling; hand check; PDC tag | High |
| LOW-22 | `listeners/FestiveMobsListener.java:50-74` | Festive hats applied to ALL zombie/skeleton spawns incl. spawner farms; named helmets enter item economy via equipment drops | Gate by SpawnReason/world | High |
| LOW-23 | `listeners/PresentListener.java:27-43`; `PresentManager.java:133-165` | Any holder can place+claim present heads anywhere (self-service); invalid-tier presents consumed with no reward on config drift | Restrict placement; preserve block on invalid tier | High |
| LOW-24 | `listeners/KrampusListener.java:60-61,71` | Debug `getLogger().info` spam on every boss hit/kidnap in production | FINE level / debug flag | High |
| LOW-25 | `christmas/SecretSantaManager.java:72-73` | Registration phase defaults open year-round (`month != 12 -> REGISTRATION`) so joins accumulate all year | Gate join window | High |
| LOW-26 | `listeners/BountyGUIListener.java:200-216` | Details GUI silently truncates >45 bounty items (no indicator) | Pagination or count line | Verified |
| LOW-27 | `events/minievents/MiniEventSession.java:174-179,231` | originalScoreboards entries leak if player quits mid-event (until end()) | Clean on quit | Verified |

---

## 9. Areas audited and found CLEAN (do not re-flag without new evidence)

- **SQL injection:** none - all JdbcStorageProvider queries use PreparedStatement placeholders; only static literals concatenated.
- **JDBC/Mongo resource handling:** try-with-resources throughout; pool/client closed on failure and shutdown; Mongo cursors auto-close.
- **Shutdown ordering:** scheduler tasks cancelled first, write executors drained (10s + forced replay), final saves last; FIFO preserves order.
- **Secret Santa deposit/claim atomicity:** confirm-slot protected vs drag/double-click/number-key; clone->save->clear with rollback; claim clears before granting; bijective derangement matching. No dupe path found.
- **Advent repeat claims:** per-UUID/per-day persisted; server-side CET time source.
- **Seasonal scavenger claims:** save-before-grant with rollback; grand-reward flag persisted before items granted; per-player found-lists keyed by UUID.
- **Bounty claim double-payout:** claimed flag set before delivery; pendingRefunds recovery state; killer!=victim checked (`BountyListener.java:37`).
- **Event offline rewards:** claim-before-execute with rollback + retry counter (#retry:N), batch persistence.
- **invsee:** read-only enforced via InventoryHolder-based cancellation.
- **Download caps & redirects:** BoundedInputStream 10MB image / 5MB NBS; non-http(s) rejected; each redirect target re-validated (subject to CRIT-04 race).
- **Placeholder injection in messages:** MessageManager uses Placeholder.unparsed/parsed; prefix/name-color sanitized on read.
- **Trade core state machine:** complete/cancel idempotent (completed/cancelled flags), synchronized; timeout task cancelled; quit/close both cancel and return offered items; acceptRequest rejects already-trading parties; self-trade blocked.
- **Black Friday math:** ingredient floor `max(1,...)` prevents zero/negative; refcounting correct.

## 10. Verification protocol for the reviewing agent

1. **Environment:** check out revision `02a50d9`. All line numbers refer to that tree.
2. **Static pass:** for each finding, re-read cited symbols; confirm excerpt accuracy; look for mitigating code the auditor may have missed elsewhere (grep broadly before rejecting).
3. **Dynamic pass (Paper 1.21+, Java 17):** prioritize CRIT-01..04, HIGH-01..10. Suggested harness: local server + test account(s) + a dummy plugin that cancels events at chosen priorities (for HIGH-01/06).
4. **Re-rate severity** after verification assuming a public SMP with untrusted players; note any finding that requires operator misconfiguration to matter.
5. **Output:** table of ID -> CONFIRMED/PARTIAL/REJECTED/NEEDS-DYNAMIC + notes; then a fix-order proposal.

## 11. Recommended fix order (original auditor's opinion)

1. CRIT-01 + CRIT-02 (lock ownership) — active theft vector
2. CRIT-03 + MED-08 (image bomb + missing permissions) — trivially triggerable crash
3. CRIT-04 (SSRF pinning)
4. HIGH-01 + HIGH-02 (trade handlers)
5. HIGH-06 + HIGH-07 (chunk loaders)
6. HIGH-08 (gamemode brick) + MED-10 (void softlock)
7. HIGH-05 (vault loss) + HIGH-04 (grave overwrite)
8. HIGH-10 (/rename sanitization) + HIGH-11 (/sudo allowlist)
9. Remaining HIGH, then MED by category, LOW opportunistically.

*End of report.*

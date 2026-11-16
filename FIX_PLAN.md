# SMPTools Fix Plan (SUPERSEDED)

> [!WARNING]
> **This plan is SUPERSEDED by [FINAL_FIX_PLAN.md](FINAL_FIX_PLAN.md).**
> Previous drafts in this document contained vulnerabilities (such as single-half lock auto-deletion on double chest pairing). Please refer to [FINAL_FIX_PLAN.md](FINAL_FIX_PLAN.md) for the authoritative, consolidated fix plan.

| | |
|---|---|
| **Status** | **SUPERSEDED** by [FINAL_FIX_PLAN.md](FINAL_FIX_PLAN.md) |
| **Companion doc** | `AUDIT_FINDINGS.md` (48 findings: 4 CRIT, 15 HIGH, 29 MED, 27 LOW) |
| **Code revision** | git `02a50d9` — all file:line refs match that tree |
| **Goal** | Fix every finding in the audit, ordered by blast radius, with regression safety |
| **Commit discipline** | One commit per finding ID (`fix(CRIT-01): ...`) so each fix is independently reviewable/revertable |

---

## 0. Guiding principles

1. **Order by blast radius**: theft/crash vectors first (Phase 1), then dupes/loss (Phase 2), then bounded abuse (Phase 3), then hygiene (Phase 4).
2. **Shared utilities before point fixes** (Phase 0): five refactors each eliminate or harden several findings at once — do them first.
3. **Config escape hatches** for any behavior change an owner might depend on:
   - new keys default to *current* behavior unless the current behavior is dangerous;
   - dangerous defaults flip only with a migration note (see §8).
4. **Event-handler rules** applied everywhere during this effort:
   - read-only scoring/logging -> `@EventHandler(priority = MONITOR, ignoreCancelled = true)`;
   - state mutation -> `HIGH`/`HIGHEST` + explicit `isCancelled()` guard, never at MONITOR;
   - GUI clicks -> identify by `InventoryHolder`, never by title string.
5. **Every fix entry below lists**: Finding ID(s), files/symbols, change spec, acceptance criteria, and (where non-obvious) a test recipe mapped to the verification steps in `AUDIT_FINDINGS.md` §10.

---

## 1. Phase 0 — Cross-cutting refactors (do first)

### R1 — `AtomicFileWriter` utility
- **Solves:** MED-01 fully; hardens MED-04, MED-29, HIGH-05, HIGH-12 persistence paths.
- **Files:** new `utils/AtomicFileWriter.java`; call sites: `utils/AsyncConfigHelper.java:104,116`, `BountyManager.java:208`, `MissionManager.java:111`, `SeasonalManager.java:153,164`, `ArtifactManager.java:176`, `VaultListener.java:34`, `GraveManager.saveGraves`, `EventManager.saveData`, `LockManager.saveLocks`.
```java
public final class AtomicFileWriter {
    public static void write(Path target, byte[] data) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE, StandardOpenOption.SYNC);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    /** For Bukkit FileConfiguration: serialize then atomic-write. */
    public static void save(FileConfiguration cfg, File file) throws IOException {
        write(file.toPath(), cfg.saveToString().getBytes(StandardCharsets.UTF_8));
    }
}
```
- **Acceptance:** no remaining direct `Files.write(path, bytes)` or `config.save(file)` on live files; kill -9 mid-write always leaves either old or new complete file.
- **Test:** loop-test on dev server: start async save, SIGKILL server repeatedly, assert yml parses every boot.

### R2 — Listener annotation & priority audit
- **Solves:** HIGH-01, HIGH-06, MED-11, MED-18 (annotations part); prevents recurrence.
- **Files:** `TradeListener.java:32,145`, `ChunkLoaderListener.java:24-54`, `LockListener.java:64-82`, `SitListener.java:33`, plus a sweep of every `@EventHandler` in `src/main/java`.
- **Spec:**
  - `TradeListener.onInventoryClick/onInventoryDrag`: add `ignoreCancelled = true` (HIGH-01).
  - `ChunkLoaderListener`: split into guard (HIGH, ignoreCancelled=true, deny if not loader item) and commit (MONITOR, ignoreCancelled=true: register/deregister + drop item) (HIGH-06).
  - `LockListener.onBlockBreakConfirm`: fold mutation into the existing HIGH handler after its access check; delete the MONITOR handler (MED-11).
  - `SitListener`: add `ignoreCancelled = true` (MED-18).
  - Sweep rule from §0.4; document exceptions inline.
- **Acceptance:** grep shows no state-mutating MONITOR handlers; no mutating handler without cancellation awareness.
- **Test:** dummy plugin cancelling BlockPlace/BlockBreak/InventoryClick at HIGHEST/MONITOR -> no phantom loaders, no beacon dupe, no trade dupe (maps to AUDIT dynamic tests for HIGH-01/HIGH-06).

### R3 — `GuiHolder` pattern (InventoryHolder-based GUI identification)
- **Solves:** MED-09, LOW-20, HIGH-15 (top-inventory check); hardens Bounty/Trade/Artifact/Seasonal/Event GUIs against title forgery.
- **Files:** new `gui/GuiHolder.java`; retrofit: `TrollGUIListener`, `AdventGUIListener`, `BountyGUIListener` (4 inventories), `ArtifactEquipmentGUI`, `TradeSession`, `HalloweenGUI`, `EasterGUI`, `SeasonalGUI`, `EventGUI`, plus audit `StatsGUIListener`, `LeaderboardGUIListener`, `MissionGUIListener` for title matching.
```java
public final class GuiHolder implements InventoryHolder {
    public enum Type { TRADE, BOUNTY_LIST, BOUNTY_DETAILS, BOUNTY_CLAIM, BOUNTY_DEPOSIT,
                       ADVENT, TROLL, ARTIFACT_POUCH, HALLOWEEN, EASTER, SEASONAL_HUB, EVENT_DASH }
    private final Type type; private final java.util.UUID viewer;
    // ctor/getters; getInventory() returns null (never used for lookup)
}
// creation:
Bukkit.createInventory(new GuiHolder(Type.ADVENT, player.getUniqueId()), 27, title);
// listener guard (replaces title.equals / startsWith):
if (!(event.getView().getTopInventory().getHolder() instanceof GuiHolder gh)) return;
if (gh.type() != GuiHolder.Type.ADVENT || !gh.viewer().equals(event.getWhoClicked().getUniqueId())) return;
if (event.getClickedInventory() != event.getView().getTopInventory()) return; // top-only semantics
```
- **Acceptance:** zero `getView().title()` comparisons remain in listeners; anvil-named chests can no longer collide with any plugin GUI.
- **Test:** name a chest "Advent Calendar"/"Troll Menu - X" via anvil, open, click -> vanilla behavior, no claim/troll fires.

### R4 — `SafeLocationFinder` utility
- **Solves:** MED-14, MED-16 (with isDead check); reusable for future teleports.
- **Files:** new `teleport/SafeLocationFinder.java`; wire into `TeleportManager.startTeleport` final hop, `HomeCommand.java:57-58`, `TpaManager.acceptRequest`.
```java
public static Location findSafe(Location loc) {
    World w = loc.getWorld();
    int x = loc.getBlockX(), z = loc.getBlockZ();
    int minY = w.getMinHeight() + 1, maxY = w.getMaxHeight() - 2;
    int y = Math.max(minY, Math.min(maxY, loc.getBlockY()));
    for (int dy : new int[]{0,1,-1,2,-2,3}) {           // prefer exact spot first
        int cy = y + dy; if (cy < minY || cy > maxY) continue;
        Block feet = w.getBlockAt(x, cy, z), head = w.getBlockAt(x, cy+1, z), ground = w.getBlockAt(x, cy-1, z);
        if (!feet.getType().isSolid() && !feet.isLiquid()
                && !head.getType().isSolid() && !head.isLiquid()
                && ground.getType().isSolid()) return new Location(w, x+0.5, cy, z+0.5);
    }
    Location spawn = w.getSpawnLocation();              // last resort
    return findSafe(spawn).equals(spawn) ? spawn.add(0,0,0) : findSafe(spawn);
}
```
Also enforce world-border containment (`w.getWorldBorder().isInside(candidate)`).

### R5 — PDC-only custom item identification
- **Solves:** LOW-12; anti-forgery groundwork for LOW-21, present heads (LOW-23).
- **Files:** `chunkloaders/ChunkLoaderManager.java` (creation ~`:228` detection fallback), `listeners/FestiveMobsListener.java:134-141`, `christmas/PresentManager.java`.
- **Spec:** stamp `NamespacedKey(plugin, "chunk_loader" | "candy_cane" | "present_tier")` in the item PDC at creation; detection reads PDC only; delete displayName/lore-equality fallbacks. Keep cosmetic name/lore purely decorative.
- **Acceptance:** anvil-renamed sugar/chest items can never trigger loader placement, candy-cane consumption, or present claims.

---

## 2. Phase 1 — CRITICAL fixes

### F-CRIT-01 — Make `getBlockKey()` pure; explicit pairing policy
- **Finding:** CRIT-01 (theft via lock migration on read path)
- **Files:** `locks/LockManager.java:120-168` (`getBlockKey`), `listeners/LockListener.java` (call sites)
- **Spec:**
  1. Delete the migration block inside `getBlockKey` (lines 124-162). The method becomes a pure canonical-key lookup: single chest -> own key; double chest -> lower-coordinate half's key; **no map mutation, no save**.
  2. Add explicit pairing resolution invoked from interaction points only (`LockListener.onPlayerInteract`, `InventoryOpenEvent` handler) *before* the access check:
```java
/** Called from interaction paths ONLY - never from getBlockKey(). */
public void resolvePairing(Block chest) {
    String doubleKey = canonicalKey(chest);            // pure lookup
    if (!(chest.getState() instanceof Chest c) || !(c.getInventory() instanceof DoubleChestInventory dci)) return;
    UUID leftOwner  = containerOwners.get(halfKey(dci, true));
    UUID rightOwner = containerOwners.get(halfKey(dci, false));
    if (containerOwners.containsKey(doubleKey)) return;          // already resolved as double
    if (leftOwner != null && rightOwner != null) {               // both halves locked by different players
        // promote lower-half owner to owner of the pair, other owner becomes trusted
        UUID owner = leftOwner; UUID other = rightOwner;
        containerOwners.put(doubleKey, owner);
        containerTrusted.computeIfAbsent(doubleKey, k -> ConcurrentHashMap.newKeySet()).add(other);
        notify(owner, other, chest); saveLocks();
    } else if (leftOwner != null || rightOwner != null) {        // exactly one stale half-lock
        UUID staleOwner = (leftOwner != null) ? leftOwner : rightOwner;
        containerOwners.remove(leftOwner != null ? halfKey(dci,true) : halfKey(dci,false));
        containerTrusted.remove(...same key...);
        notifyStale(staleOwner, chest);   // "your locked chest was paired with another chest; its lock was removed"
        saveLocks();
    }
}
```
  3. Rationale: a stale single-half lock must NEVER silently become ownership of the neighbor's contents. Removing it (with notification) is fail-safe; both-locked case preserves both parties' access (owner + trusted).
- **Acceptance:** calling `getBlockKey` twice with no interactions leaves `containerOwners`/`containerTrusted` byte-identical; the AUDIT 3-step exploit ends with the victim still owning their items and the attacker's lock removed.
- **Test:** unit test purity (map snapshot before/after); dynamic repro from AUDIT CRIT-01 verification steps.

### F-CRIT-02 — Ownership guard inside `lockContainer`; break-your-own-half rule
- **Finding:** CRIT-02 (+ supports CRIT-01 fix)
- **Files:** `locks/LockManager.java:272-280`, `commands/LockCommand.java:59-63`, `listeners/LockListener.java:50-62`
- **Spec:**
```java
public boolean lockContainer(Block block, Player owner) {
    String key = getBlockKey(block);                       // now pure
    UUID existing = containerOwners.get(key);
    if (existing != null && !existing.equals(owner.getUniqueId())
            && !owner.hasPermission("smptools.locks.admin")) {
        return false;                                      // command maps this to an error message
    }
    ...
}
```
  - In `LockListener.onBlockBreak` deny branch: allow the break if the breaker owns EITHER half-key of the paired chest (their own half); after break, existing `removeOrMigrateLock` cleanup runs for the surviving half.
  - Keep the `LockCommand` pre-check for UX messaging but it is no longer load-bearing.
- **Acceptance:** locking one half of an unlocked pair claims only via canonical key but refuses when ANY owner exists that isn't you/admin; victim can always break their own half.
- **Test:** dynamic scenarios from AUDIT CRIT-02.

### F-CRIT-03 — Image dimension pre-check before decode
- **Finding:** CRIT-03 (decompression bomb OOM)
- **Files:** `imagemap/ImageProcessor.java:17-24`; constant in `utils/Constants.java`
- **Spec:**
```java
private static final long MAX_PIXELS = 4096L * 4096L;

public static BufferedImage getImage(InputStream inputStream, int width, int height) {
    try {
        ImageInputStream iin = ImageIO.createImageInputStream(inputStream);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iin);
        if (!readers.hasNext()) return null;
        ImageReader reader = readers.next();
        try {
            reader.setInput(iin, true, true);
            long pw = reader.getWidth(0), ph = reader.getHeight(0);
            if (pw <= 0 || ph <= 0 || pw * ph > MAX_PIXELS) return null;   // reject BEFORE allocation
            return reader.read(0);
        } finally { reader.dispose(); }
    } catch (Exception e) { log...; return null; }
    // downstream resize/dither unchanged
}
```
  - Keep the existing BoundedInputStream size cap (defense in depth).
- **Acceptance:** crafted 30000x30000 PNG returns null + log line; normal images unaffected; heap profile flat during rejection.
- **Test:** generate bomb PNG offline, `/tomap` it on small-heap dev server -> server survives, player gets error message.

### F-CRIT-04 — SSRF: resolve-once pinning + expanded blocked ranges
- **Finding:** CRIT-04 (+ complements MED-05)
- **Files:** `utils/URLValidator.java` (validation ~`:84`, connect path ~`:115`, range check `:99-104`), call sites `MapCommand.java:37-43,71`, `MusicCommand.java:92-98,104`
- **Spec:**
  1. Expand `isBlockedAddress`: add IPv6 ULA `fc00::/7` (first byte 0xFC/0xFD), CGNAT `100.64.0.0/10`, benchmarking `198.18.0.0/15`, multicast (`isMulticastAddress`), `192.0.0.0/24`, documentation ranges, unspecified/loopback/link-local/site-local as today.
  2. Pin DNS: resolve once, validate ALL addresses, connect to a validated address so the URL never re-resolves:
```java
InetAddress[] all = InetAddress.getAllByName(host);
List<InetAddress> allowed = Arrays.stream(all).filter(a -> !isBlockedAddress(a)).toList();
if (allowed.isEmpty()) throw new IOException("host resolves only to blocked addresses");
InetAddress pinned = allowed.get(0);

URL url2 = new URL(new URL(urlStr), "/" + rawPath + (q != null ? "?" + q : "")); // scheme/host kept for Host header & SNI
HttpsURLConnection conn = (HttpsURLConnection) url2.openConnection(
        new Proxy(Proxy.Type.HTTP, new InetSocketAddress(pinned.getHostAddress(), port))); // see note
```
  Implementation note: JDK has no first-class "connect to this IP" for HttpsURLConnection; two workable options —
  - **Option A (recommended):** custom `SSLSocketFactory`/`SocketFactory` whose `createSocket()` connects to the pinned `InetSocketAddress` (validate hostname against SNI manually or accept because we validated origin intent), OR
  - **Option B:** perform the fetch with a raw `Socket` + minimal HTTP client for GET-only image/NBS downloads (no cookies/auth needed here).
  Apply the same factory to redirect-following hops; each redirect re-runs full validation + re-pinning (existing loop at `:118,137-138`).
  3. Move validation+fetch fully into the async task (fixes MED-05 simultaneously): command handler schedules async immediately; sync DNS disappears from main thread.
- **Acceptance:** TTL-0 rebinding host cannot reach loopback/metadata; direct requests to `fd00::1`, `100.64.0.1`, `198.18.0.1` rejected; public HTTPS images still load; main thread shows no DNS activity during `/tomap`.
- **Test:** local DNS server / /etc/hosts alternation repro from AUDIT; unit tests for new blocked ranges.

---

## 3. Phase 2 — HIGH fixes

### F-HIGH-01/02 — Trade listener hardening + post-completion guard
- **Findings:** HIGH-01 (dupe via later cancel), HIGH-02 (1-tick interactive window / decoration theft)
- **Files:** `listeners/TradeListener.java:32,145`, `trade/TradeSession.java:190-212`
- **Spec:**
  1. `ignoreCancelled = true` on both handlers (R2).
  2. In `TradeSession.completeTrade()`/`cancelTrade()`: before returning items, blank ALL button/decoration slots (set null) so nothing is grabbable; keep session resolvable until close:
     - add `TradeSession.state` enum {ACTIVE, SETTLING, DONE}; `removeSession` moves to a `settlingSessions` map keyed by both UUIDs;
     - `TradeListener.onInventoryClick`: if session is in `settlingSessions` -> cancel event unconditionally and return;
     - the scheduled close task removes entries from `settlingSessions`.
  3. Shift-click transfer: only mutate inventories after re-checking `!event.isCancelled()`; wrap mutations so a later cancel cannot strand them (with R2's ignoreCancelled this is belt-and-braces).
- **Acceptance:** with a dummy plugin cancelling clicks at HIGHEST/MONITOR, no dupe; clicking any top slot during settle tick cancels; barrier/panes unobtainable.
- **Test:** AUDIT HIGH-01/HIGH-02 dynamic recipes.

### F-HIGH-03/04 — Grave lifecycle
- **Findings:** HIGH-03 (orphaned graves, replacement-head looting), HIGH-04 (same-location overwrite)
- **Files:** `graves/GraveManager.java` (add handlers; modify death flow `:62-122`)
- **Spec:**
  1. New `@EventHandler(priority=HIGH, ignoreCancelled=true) onGraveBlockBreak(BlockBreakEvent)`: if `graves.containsKey(loc)`:
     - owner or admin (`smptools.graves.admin`) breaking -> finalize: drop items at location, remove holograms, remove entry, save (grave "pops" like vanilla);
     - anyone else -> cancel + message.
  2. Same treatment for `BlockExplodeEvent`/`EntityExplodeEvent` block lists (remove grave blocks from list OR finalize them).
  3. Stamp placed head with PDC `NamespacedKey(plugin,"grave_owner")` = owner UUID; interact handler verifies PDC matches map entry (kills replacement-head looting even if maps desync).
  4. Overwrite fix: in death flow, if `graves.containsKey(validLocation)` continue the existing dy-scan to the next free candidate before giving up; only merge into existing grave as last resort (append items, update hologram count).
  5. Add expiry: config `features.player-graves.expire-hours` (default e.g. 72); periodic hourly task finalizes expired graves (drop items at grave location so nothing is destroyed).
- **Acceptance:** breaking someone else's grave head impossible; owner break pops items; explosion never orphans; double-death same spot yields two reachable graves; old graves expire.
- **Test:** AUDIT HIGH-03/04 steps + expiry fast-forward via short config value.

### F-HIGH-05 — Vault decode-failure safety
- **Finding:** HIGH-05
- **Files:** `commands/PrivateVaultCommand.java:42-76`, `listeners/VaultListener.java:31-35`
- **Spec:**
  1. Track decode failure per open: `openVault` returns boolean / sets player transient flag `vaultDecodeFailed`.
  2. `VaultListener.onInventoryClose`: if flag set -> do NOT write blob; notify player "vault could not be loaded; contents unchanged; contact staff"; log severe with UUID; clear flag.
  3. Bound `readInt()`: reject `n < 0 || n > configuredVaultSize*4`; on violation throw -> same failure path.
  4. Migration (follow-up): store vaults as YAML `ItemStack[]` via ConfigurationSerializable in own file `vaults.yml` through R1 writer; one-time converter reads legacy blobs read-only (never overwrites until first successful decode+save).
- **Acceptance:** corrupted blob -> vault opens read-only-ish (empty view) but original blob preserved forever; OOM impossible via crafted int.
- **Test:** hand-corrupt blob in config.yml, open+close vault, verify blob unchanged; craft huge-int blob, verify clean rejection.

### F-HIGH-06/07 — Chunk loader lifecycle
- **Findings:** HIGH-06 (cancellation dupes), HIGH-07 (caps/explosions/ownership)
- **Files:** `listeners/ChunkLoaderListener.java:24-54`, `chunkloaders/ChunkLoaderManager.java`
- **Spec:**
  1. R2 split: guard at HIGH (ignoreCancelled), commit at MONITOR (register/deregister/drop).
  2. Manager changes:
     - store records `{location, ownerUUID, placedAt}` instead of bare locations;
     - caps: `features.chunkloaders.max-per-player` (default 8), `max-global` (default 128); deny place with message when exceeded;
     - new handlers `EntityExplodeEvent`/`BlockExplodeEvent`: for each removed loader-block position call `removeChunkLoader` (beacon drops naturally from explosion — acceptable);
     - `EntityChangeBlockEvent` (wither): if block is registered loader -> cancel;
     - ownership: only owner/admin may break a loader (guard checks record owner);
     - admin subcommands `/givechunkloader list|clear <player>` for cleanup.
  3. Data migration: existing `chunkloaders.yml` entries get `owner: null` treated as "server-owned" (admin-breakable, counts toward global cap only).
- **Acceptance:** phantom loaders impossible; TNT'd loaders deregister; cap enforced; non-owner cannot break.
- **Test:** AUDIT HIGH-06 recipe with dummy cancelling plugin; cap/explosion scenarios scripted.

### F-HIGH-08 — Christmas gamemode save/restore
- **Finding:** HIGH-08 (+ MED-10 void fix)
- **Files:** `listeners/ChristmasWorldListener.java:40-66`, `PortalListener.java:53`
- **Spec:**
```java
private static final Map<UUID, GameMode> previousMode = new HashMap<>();   // main-thread only

onPlayerChangedWorld(event):
    if entering christmas:
        if (!previousMode.containsKey(id)) previousMode.put(id, player.getGameMode());
        if (previousMode.get(id) != SPECTATOR) player.setGameMode(ADVENTURE);
    else if leaving christmas && previousMode.containsKey(id):
        player.setGameMode(previousMode.remove(id));

// also restore on PlayerQuitEvent (persist intended mode via PDC byte so relog mid-world still restores),
// and on PlayerDeathEvent respawn in overworld.
```
  - Void fix (MED-10): `if (event.getCause() != DamageCause.VOID) event.setCancelled(true);` plus scheduled check: player below `world.getMinHeight()` -> teleport to christmas spawn.
- **Acceptance:** enter->leave restores exact prior mode incl. across relog; void fall teleports up instead of softlocking.
- **Test:** survival enter/exit/relog/death matrix; dig to void hole in flat christmas world.

### F-HIGH-09 — Krampus guard liveness tracking
- **Finding:** HIGH-09
- **Files:** `listeners/KrampusListener.java:100-109`, `christmas/KrampusManager.java:292-305`
- **Spec:**
  1. Replace killer-identity condition with liveness: `EntityDeathEvent` for any entity tagged cage-guard -> `checkGuardDeath(victimId, guardId)` regardless of killer; also `EntitiesUnloadEvent`/periodic task validating `Bukkit.getEntity(guardId) == null` -> treat as dead.
  2. Keep victim-kill bonus message only when `entity.getKiller()` is the victim (cosmetic path).
  3. Safety valve: periodic task every 10s per kidnapped player — if zero live guards remain -> `releasePlayer`.
- **Acceptance:** `/kill @e[type=!player]` mid-cage still frees victim; normal kill flow unchanged.
- **Test:** kidnap self via debug command, clear guards via console, assert release within 10s.

### F-HIGH-10 — `/rename` gating & sanitization
- **Finding:** HIGH-10
- **Files:** `commands/RenameCommand.java:41-45`, `plugin.yml:63-65`, reuse `utils/InputValidator.sanitizeMiniMessage()`
- **Spec:**
  1. plugin.yml: add `permission: smptools.rename` (default true — preserves availability while making it manageable; document in README permissions table).
  2. Code: length cap 64 chars raw; sanitize: strip `<click:...>`, `<hover:...>`, `<insert:...>`, `<javascript:>`, `<font:...>` tags (extend sanitizer with a tag blacklist), keep colors/decorations.
  3. Apply identical sanitization where tags/titles reach item-free chat surfaces (LOW-11 companion).
- **Acceptance:** `<click:run_command:'/pay ...'>` renders as inert text; colored names still work; >64 chars rejected with message.
- **Test:** rename attempts from AUDIT HIGH-10; death-message click test between two accounts.

### F-HIGH-11 — `/sudo` allowlist + target protection
- **Finding:** HIGH-11
- **Files:** `commands/SudoCommand.java:42-52`, `utils/CommandBlacklist.java`, `plugin.yml:109-111`
- **Spec:**
  1. Convert blacklist -> allowlist config `sudo.allowed-command-prefixes` (default: `[msg, tell, w, me, help]`) — sudo becomes a moderation/roleplay tool by default; ops can extend deliberately.
  2. Refuse targets that are op OR hold `smptools.sudo` unless sender confirms by repeating the full command with `--confirm` prefix within 15s.
  3. Add `permission: smptools.sudo` key to plugin.yml command section.
- **Acceptance:** `/sudo X lp user Y parent add admin` blocked (not in allowlist); `/sudo X msg hi` works; targeting another sudo-holder requires --confirm.
- **Test:** matrix of allowed/blocked prefixes + confirm flow.

### F-HIGH-12 — Black Friday discount persistence
- **Finding:** HIGH-12
- **Files:** `listeners/BlackFridayListener.java:26,56-64`, new file `blackfriday_state.yml` via R1
- **Spec:** on apply: persist per-villager `{uuid, originalRecipes serialized}` snapshot; on startup (before any listener applies discounts): if state file exists -> restore originals then delete file; on clean stop(): restore + delete. Keep in-memory refcount logic unchanged.
- **Acceptance:** SIGKILL during event -> next boot villagers trade at normal prices; event restartable.
- **Test:** apply discount, SIGKILL, boot, inspect trades.

### F-HIGH-13 — Dialogue cleanup on disconnect
- **Finding:** HIGH-13
- **Files:** `managers/DialogueManager.java:126-142` + new quit hook registration (wherever DialogueManager listeners register)
- **Spec:** watchdog offline branch calls `removeTextDisplay(player)` before cancel; add `PlayerQuitEvent` handler calling `stopDialogue(player)` (which already removes display + clears the four UUID-keyed maps at `:31-34`).
- **Acceptance:** join/click/NPC/quit x50 leaves zero TextDisplays (`/execute if entity @e[type=text_display]` count stable).
- **Test:** repeat cycle script; entity count assertion.

### F-HIGH-14 — Confine flatfile config access to main thread
- **Finding:** HIGH-14
- **Files:** `tags/TagManager.java:87-109`, `storage/FlatFileStorageProvider.java:95-121`
- **Spec:** mirror the correct pattern already used by `snapshotLegacyTitlesSync()`:
  - async task gathers UUIDs, then `callSyncMethod` (timed `.get(5, SECONDS)` — see LOW-02) performs `getAllPlayerTitles`/`savePlayerTitle` on main thread;
  - alternatively cache titles in the existing `ConcurrentHashMap playerTitles` and make FlatFile provider methods assert main-thread (`Bukkit.isPrimaryThread()`) with fail-loud logging.
  Choose option A (sync-hop) to avoid API behavior change.
- **Acceptance:** no YamlConfiguration access off-main (assert via temporary `Bukkit.isPrimaryThread()` checks left in debug mode); no CME under load test.
- **Test:** 200-player simulated title unlock storm while reload runs.

### F-HIGH-15 — Advent GUI top-inventory semantics
- **Finding:** HIGH-15 (with R3 holder retrofit)
- **Files:** `listeners/AdventGUIListener.java:89-101`
- **Spec:** after R3 guard, derive day from clicked item PDC (`advent_day` INT stamped at GUI build) instead of slot math; ignore clicks outside top inventory entirely.
- **Acceptance:** hotbar clicks do nothing; claim only fires on real day icons; future days still blocked.
- **Test:** AUDIT HIGH-15 repro (dirt in hotbar slot 5).

---

## 4. Phase 3 — MEDIUM fixes (batched by subsystem)

### Storage & persistence
- **MED-02** (`JdbcStorageProvider.java:52-57`): flip default `use-ssl=true`; append `&sslMode=REQUIRED` guidance comment in `config.yml` defaults; migration note in §8.
- **MED-03** (`JdbcStorageProvider.java:252-293`, `MongoStorageProvider.java:202-234`): add same-thread pending-write cache (`ConcurrentHashMap<statKey, value>`) consulted by reads before hitting DB; cleared on write-executor completion callback. Document residual cross-node staleness.
- **MED-04** (`VaultListener.java:32-34`): move vaults to dedicated `vaults.yml` (pairs with F-HIGH-05 migration); save via R1 on close + periodic 30s dirty-flush while open.

### Commands & network surface
- **MED-05**: solved inside F-CRIT-04 step 3 (async validation). Verify no sync DNS remains.
- **MED-06** (`NBSParser.java:78-79`, `SongPlayer.java:37`): parser rejects/clamps `key` outside 33..57 and instrument >15 at read time; SongPlayer additionally wraps `playNote` in try/catch with per-song error counter (abort song after 10).
- **MED-07** (`EnchantmentListener.java:94-111`): replace recursion with iterative queue capped at 512 blocks (copy the cap pattern from `ArtifactListener.fellTree`); over-cap -> break only collected portion.
- **MED-08** (`plugin.yml:73-78`, `MusicCommand`, `MapCommand`, `MapManager.java:24-57`): add `smptools.tomap` / `smptools.music` permission keys (default true to preserve behavior; ops can restrict); per-player cooldown map (10s) for both commands; cap stored maps per player (default 32) enforced in MapCommand before persisting.

### Locks (additional)
- **MED-12** (`locks/LockManager.java:238-251`): exclude `Material.ENDER_CHEST` explicitly in `isContainer` so the public ender chest can never be locked/break-protected by a player.
- **MED-13** (`listeners/LockListener.java:106-114`): add `EntityChangeBlockEvent` handler (HIGH, ignoreCancelled=true) — if `event.getBlock()` is a locked container and the entity is a Wither (or any block-changing mob), cancel the event; keeps wither fights from eating locked chests.

### GUIs
- **MED-09**: solved by R3 retrofit of TrollGUIListener + re-check `smptools.troll` inside click branch.

### Christmas / seasonal
- **MED-10**: solved in F-HIGH-08.
- **MED-22** (`SnowballListener.java:36-57`): gate entire handler behind world check (`player.getWorld().getName().equalsIgnoreCase(christmasWorld)`) AND config date-window check reusing SeasonalManager season detection; exclude tamed animals/villagers; headshot band `[eyeY - 0.3, eyeY + 0.4]`.
- **MED-23** (`SecretSantaManager.java:98-123`): `admin start` refuses when any `gifts.*` key exists (error listing count); new `admin clear` subcommand required first; generation skips offline participants and logs them.
- **MED-24** (`KrampusManager.java:185-224,307-329`): periodic validation task (every 15s): kidnapped player offline/far (>20 blocks from cage center)/guards all dead -> releasePlayer without forced teleport (make release teleport optional config `krampus.release-teleport: false` default); cancel chorus fruit/ender pearl consumption while kidnapped (PlayerItemConsumeEvent/ProjectileLaunchEvent guards).

### Teleport & TPA
- **MED-14**: solved by R4 wiring.
- **MED-15** (`TeleportListener.java:24-43`): add `PlayerInteractEntityEvent`/`VehicleEnterEvent` handler cancelling pending warmup when player mounts anything; also cancel on `EntityDismountEvent`? No — mounting is the bypass; dismount is fine.
- **MED-16** (`TpaManager.java:112-129`): accept path adds `requester.isDead()` rejection with message; new `PlayerDeathEvent` handler calls `teleportManager.cancelTeleport(player)` for both parties of any pending teleport.

### Sleep & AFK
- **MED-17** (`SleepManager.java:46-108`): `addVote` ignores players not in initiator's world; `checkVoteStatus` drops votes of offline/non-world players; AFK auto-yes becomes config `features.afk.auto-vote-sleep` (default flips to false — see §8).

### Sitting / flight
- **MED-18**: annotation part in R2; hand check `if (event.getHand() != EquipmentSlot.HAND) return;` in SitListener.
- **MED-19** (`FlyCommand.java:20-28`): policy hooks — on PlayerDeathEvent and PlayerChangedWorldEvent away from origin world, if `bootsFlightGranted`-style plugin-granted flight (track set keyed by UUID for `/fly` too) -> revoke unless `features.fly.persist-across-death: true` (default true = current behavior, documented); guard creative/spectator from being stripped.

### NPCs
- **MED-20** (`NPCManager.java:156-159`, `NPCCommand.java:37`): validate skinName against `[A-Za-z0-9_]{1,16}` and id against `[a-z0-9_-]{1,32}` before use; reject otherwise with message. Replace string-built `data merge` with profile API if available on the target Paper version; keep merge as fallback but only post-validation.
- **MED-21** (`NPCManager.java:147-155,259-260`): move `profile.complete()` into async task; apply skin via sync continuation; startup spawns NPC body immediately, skin applies when fetch completes.

### Events engine
- **MED-25** (`ArtifactListener.java:225-245`): mirror `fellTree` pattern — fire synthetic `BlockBreakEvent` per crop, skip on cancel, then `breakNaturally` + replant.
- **MED-26** (`MiniEventSession.java:272-318`): on `executeReward` failure for ONLINE winner -> enqueue into `eventManager.queueOfflineRewardsBatch` retry queue instead of log-only.
- **MED-27** (`MiniEventSession.java:165-208`): track replaced boards explicitly (`Map<UUID, Scoreboard> replacedBoards`); always restore own board on quit hook; unregister smpevent objective from foreign boards on end; clean originalScoreboards on PlayerQuitEvent.
- **MED-28** (`MiniEventListener.java:232-311`): add optional config `events.types.mob_frenzy.count-spawner-mobs: false` — when false, check `entity.getSpawnReason()==SpawnReason.SPAWNER` via spawn-tracking metadata stamped in a MonsterSpawnListener (only during active session to avoid overhead).
- **MED-29** (`BountyManager.java:322-352`): claim path uses synchronous `saveBountiesSync()` (already exists) instead of async; keep tombstone: claimed bounties written as `claimed: true` records until refund fully delivered, then compacted (adjust `writeBountiesToFile:184-186` omission rule accordingly).

---

## 5. Phase 4 — LOW fixes (quick-hit table)

| ID | Change |
|----|--------|
| LOW-01 | `AsyncConfigHelper`: once `shuttingDown`, run saves inline instead of resurrecting executor; remove dead flag or use it; drop monitor-held drain (poll `awaitTermination` without holding class lock) |
| LOW-02 | Replace `.get()` with `.get(5, TimeUnit.SECONDS)` + timeout fallback in `TrailManager.java:87-89`, `TagManager.java:134,186-188` |
| LOW-03 | Replace async `player.isOnline()` reads with `Bukkit.getPlayer(uuid) != null` pattern (as TagManager:231 does) in TrailsGUIListener:32, TrailManager:98 |
| LOW-04 | Mark `LeaderboardManager.lastCacheTime` volatile |
| LOW-05 | MongoStorageProvider: make legacy `_` fallback symmetric in getAllPlayerStats/loadAllPlayerStats; add one-time migration rewriting `_` keys to `$DOT$` form |
| LOW-06 | Add save-on-quit for stats (JoinLeaveListener calls stats flush) behind config `stats.save-on-quit: true` default true |
| LOW-07 | CustomItemCommand: guard `material.isItem()` + null meta before use |
| LOW-08 | MapCommand error path: send generic "could not load image" to player; log full exception server-side |
| LOW-09 | MusicCommand: PlayerQuitEvent cancels that player's playingTasks entry |
| LOW-10 | SoundCommand: per-player 5s cooldown map |
| LOW-11 | ChatManager/NameTagListener: pass tagTitle through sanitizeMiniMessage; clean lastMessengers on quit (pairs LOW-17) |
| LOW-12 | Solved by R5 |
| LOW-13 | PortalListener: early-return unless block coords changed (copy TeleportListener:37-38 pattern); resolve overworld via `Bukkit.getWorlds().stream().filter(w -> w.getEnvironment()==NORMAL).findFirst()`; add vehicle/dead guards |
| LOW-14 | LockListener hopper/dispenser: allow passthrough when the moving player context is the owner where API exposes it; otherwise document sealed behavior in README |
| LOW-15 | LockManager.isContainer: add CRAFTER, LECTERN, DECORATED_POT (+ DECORATED_POT break protection) |
| LOW-16 | CropAccelerator: manual age-increment logic for cane/cactus/bamboo; cap processed chunks per tick (e.g. 16) round-robin |
| LOW-17 | Covered by LOW-11 quit cleanup |
| LOW-18 | TpaManager: clear tpaToggledOff on quit; key pendingRequests by `target->Map<requester,request>`; reject duplicate requester instead of blocking all senders |
| LOW-19 | DailyRewardCommand: wrap Instant.parse in try/catch -> treat as never-claimed + log warning |
| LOW-20 | Solved by R3 |
| LOW-21 | FestiveMobsListener candy cane: PDC tag detection (R5); hand check; replace `(TextComponent) displayName).content()` with PlainTextComponentSerializer |
| LOW-22 | FestiveMobsListener hats: only on `SpawnReason.NATURAL` and overworld/christmas worlds |
| LOW-23 | PresentListener/PresentManager: placement requires christmas world OR hunt flag; invalid tier -> keep block + warn owner, do not consume registry entry |
| LOW-24 | KrampusListener: demote logs to `Level.FINE` / debug-flag gated |
| LOW-25 | SecretSantaManager: join allowed only when month == 12 (config `secretsanta.open-year-round: false`) |
| LOW-26 | BountyGUIListener details GUI: add lore line "+N more items not shown" when truncated at 45 |
| LOW-27 | MiniEventSession: PlayerQuitEvent removes originalScoreboards/replacedBoards entries |

---

## 6. Testing & rollout

### Per-phase regression checklist
Run after each phase; full suite after Phase 2:

1. **Trade:** complete trade both directions; cancel mid-trade returns items; quit/death/kick mid-trade returns items; shift-click stack merges; timeout fires; no dupe with dummy-canceller plugin active.
2. **Locks:** lock/unlock/trust/untrust persist across restart; double-chest scenarios from AUDIT CRIT-01/02 all fail to steal; explosions preserve locked chests; hopper minecarts blocked.
3. **Graves:** death->loot flow; owner-break pops; second-death-same-spot yields two graves; expiry drops items.
4. **Vaults:** normal open/close roundtrip byte-stable; corrupted blob never overwritten.
5. **Chunk loaders:** place/break/cap/explosion/wither matrix.
6. **Christmas:** enter/exit gamemode matrix incl. relog/death; void fall rescue; Krampus kidnap->console-clear-guards->auto-release; Secret Santa full lifecycle incl. admin start refusal when gifts exist; Black Friday crash-restart restore.
7. **Events:** each of 6 event types scores correctly; rewards delivered online+offline; failure path enqueues retry; scoreboard restores for custom-board players and quitters.
8. **Network:** `/tomap` normal image OK; bomb PNG rejected; rebinding host blocked; internal-range hosts blocked; `/music` NBS plays; malformed NBS aborted cleanly.
9. **Storage:** flatfile + sqlite + mongodb smoke test of stats/tags/trails/bounties round-trip; SIGKILL-mid-save leaves valid yml.

### Config migration notes (§0.3 escape hatches)
| Key | Change | Migration |
|-----|--------|-----------|
| `storage.jdbc.use-ssl` | default false -> true (MED-02) | Owners with plaintext-DB setups must set false explicitly; log WARNING when false |
| `features.afk.auto-vote-sleep` | default true -> false (MED-17) | Changelog note; opt back in deliberately |
| `smptools.rename`, `smptools.tomap`, `smptools.music` | new permission nodes, default true (HIGH-10/MED-08) | No action needed until owners restrict |
| `sudo.allowed-command-prefixes` | new allowlist replaces blacklist (HIGH-11) | Ops port any previously-blocked-but-needed commands explicitly |
| `features.chunkloaders.max-per-player/-global` | new caps (HIGH-07) | Defaults generous (8/128); existing loaders grandfathered as server-owned |
| `features.player-graves.expire-hours` | new expiry (F-HIGH-03) | Default 72h; set -1 to disable |

### Commit granularity
One commit per finding/refactor ID in this plan's order (R1..R5, F-CRIT-01..04, F-HIGH-*, MED batches may group same-file fixes into one commit each, LOW table may batch by subsystem). Each commit message references the AUDIT_FINDINGS.md ID.

### Definition of done
- All 48 IDs appear in commits (or are explicitly closed as WONTFIX-with-rationale in a final doc appended to AUDIT_FINDINGS.md).
- Full regression checklist green on Paper 1.21.x, Java 17.
- `mvn clean package` + `mvn test` green.

*End of plan.*

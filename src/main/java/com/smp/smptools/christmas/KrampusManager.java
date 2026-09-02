package com.smp.smptools.christmas;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class KrampusManager implements Listener {

    private final SMPTools plugin;
    private FileConfiguration christmasConfig;
    private final Map<UUID, Location> kidnappedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Location> playerCages = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerGuards = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Location, org.bukkit.block.BlockState>> originalCageBlocks = new ConcurrentHashMap<>();
    private final Set<UUID> allowedTeleports = ConcurrentHashMap.newKeySet();
    public final NamespacedKey krampusKey;

    public boolean isAllowedTeleport(UUID uuid) {
        return uuid != null && allowedTeleports.contains(uuid);
    }

    public KrampusManager(SMPTools plugin) {
        this.plugin = plugin;
        this.krampusKey = new NamespacedKey(plugin, "krampus_entity");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        loadConfig();
        startGuardWatchdogTask();
    }

    private void startGuardWatchdogTask() {
        if (plugin == null || !plugin.isEnabled()) return;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, Set<UUID>> entry : new HashMap<>(playerGuards).entrySet()) {
                UUID victimUuid = entry.getKey();
                Player victim = Bukkit.getPlayer(victimUuid);
                if (victim == null || !victim.isOnline()) {
                    releasePlayer(victimUuid);
                    continue;
                }

                Set<UUID> guards = entry.getValue();
                guards.removeIf(guardId -> {
                    org.bukkit.entity.Entity e = Bukkit.getEntity(guardId);
                    return e == null || e.isDead() || !e.isValid();
                });

                if (guards.isEmpty()) {
                    releasePlayer(victimUuid);
                }
            }
        }, 200L, 200L); // Watchdog every 10s
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        cleanupStaleCages();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "christmas.yml");
        if (file.exists()) {
            christmasConfig = YamlConfiguration.loadConfiguration(file);
        } else {
            christmasConfig = new YamlConfiguration();
        }
        cleanupStaleCages();
    }

    private void cleanupStaleCages() {
        if (christmasConfig == null || !christmasConfig.contains("active_cages")) return;
        org.bukkit.configuration.ConfigurationSection section = christmasConfig.getConfigurationSection("active_cages");
        if (section == null) return;

        List<String> cleanedKeys = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            UUID cageUuid = null;
            try {
                cageUuid = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {}
            if (cageUuid != null && (playerCages.containsKey(cageUuid) || kidnappedPlayers.containsKey(cageUuid))) {
                // Live active cage currently managed in memory, do not clean up
                continue;
            }

            String worldName = section.getString(key + ".world");
            if (worldName == null) {
                cleanedKeys.add(key);
                continue;
            }
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                // World not loaded yet - retain entry for later world load cleanup
                continue;
            }

            int cx = section.getInt(key + ".x");
            int cy = section.getInt(key + ".y");
            int cz = section.getInt(key + ".z");
            Location center = new Location(world, cx, cy, cz);

            for (int x = -4; x <= 4; x++) {
                for (int y = 0; y <= 5; y++) {
                    for (int z = -4; z <= 4; z++) {
                        org.bukkit.block.Block b = center.clone().add(x, y, z).getBlock();
                        if (isCageFrameBlock(b, x, y, z)) {
                            b.setType(Material.AIR);
                        }
                    }
                }
            }
            cleanedKeys.add(key);
        }

        if (!cleanedKeys.isEmpty()) {
            for (String key : cleanedKeys) {
                christmasConfig.set("active_cages." + key, null);
            }
            if (section.getKeys(false).isEmpty()) {
                christmasConfig.set("active_cages", null);
            }
            saveChristmasConfig();
        }
    }

    private boolean isCageFrameBlock(org.bukkit.block.Block b, int x, int y, int z) {
        if (b == null) return false;
        if (y == 0 && Math.abs(x) <= 3 && Math.abs(z) <= 3) {
            return b.getType() == Material.BEDROCK;
        }
        if (x == -4 || x == 4 || z == -4 || z == 4 || y == 0 || y == 5) {
            return b.getType() == Material.IRON_BARS;
        }
        return false;
    }

    private void saveCageLocation(UUID uuid, Location center) {
        if (christmasConfig == null || center == null || center.getWorld() == null) return;
        String path = "active_cages." + uuid.toString();
        christmasConfig.set(path + ".world", center.getWorld().getName());
        christmasConfig.set(path + ".x", center.getBlockX());
        christmasConfig.set(path + ".y", center.getBlockY());
        christmasConfig.set(path + ".z", center.getBlockZ());
        saveChristmasConfig();
    }

    private void removeCageLocation(UUID uuid) {
        if (christmasConfig == null) return;
        christmasConfig.set("active_cages." + uuid.toString(), null);
        saveChristmasConfig();
    }

    private void saveChristmasConfig() {
        if (christmasConfig == null) return;
        try {
            File file = new File(plugin.getDataFolder(), "christmas.yml");
            christmasConfig.save(file);
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to save christmas config: " + e.getMessage());
            }
        }
    }

    public void spawnKrampus(Location location) {
        WitherSkeleton krampus = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);

        // Stats
        double health = christmasConfig.getDouble("krampus.health", 300.0);
        krampus.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        krampus.setHealth(health);

        // Name
        String name = christmasConfig.getString("krampus.name", "&c&lKrampus");
        krampus.customName(LegacyComponentSerializer.legacySection().deserialize(name.replace("&", "§")));
        krampus.setCustomNameVisible(true);

        // Equipment
        krampus.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        krampus.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        krampus.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));

        // Persistent Data
        krampus.getPersistentDataContainer().set(krampusKey, PersistentDataType.BYTE, (byte) 1);
    }

    public void kidnapPlayer(Player player, WitherSkeleton krampus) {
        if (player == null || kidnappedPlayers.containsKey(player.getUniqueId()))
            return;

        // Find non-overlapping Cage Location (clamped to world bounds) before removing Krampus
        Location cageLoc = findNonOverlappingCageLocation(player.getLocation());
        if (cageLoc == null) {
            if (plugin != null) {
                plugin.getLogger().warning("Could not find a non-overlapping cage location to kidnap " + player.getName() + "; preserving Krampus encounter.");
            }
            return;
        }

        // Despawn Krampus only after confirming valid cage placement
        if (krampus != null) {
            krampus.remove();
        }

        // Save location
        kidnappedPlayers.put(player.getUniqueId(), player.getLocation());
        playerCages.put(player.getUniqueId(), cageLoc);

        // Build Cage with original block state snapshot
        buildCage(player.getUniqueId(), cageLoc);

        // Teleport
        try {
            allowedTeleports.add(player.getUniqueId());
            player.teleport(cageLoc.clone().add(0.5, 1, 0.5));
        } finally {
            allowedTeleports.remove(player.getUniqueId());
        }
        player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("krampus.kidnapped"));

        // Spawn Guards
        Set<UUID> guards = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Zombie guard = (Zombie) cageLoc.getWorld().spawnEntity(cageLoc.clone().add(0.5, 1, 0.5), EntityType.ZOMBIE);
            guard.customName(Component.text("Cage Guard", NamedTextColor.RED));
            guard.setCustomNameVisible(true);
            guard.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            guards.add(guard.getUniqueId());
        }
        playerGuards.put(player.getUniqueId(), guards);
    }

    private Location findNonOverlappingCageLocation(Location baseLoc) {
        if (baseLoc == null || baseLoc.getWorld() == null) return null;
        org.bukkit.World world = baseLoc.getWorld();
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 6;

        int[] horizontalOffsets = {0, 20, -20, 40, -40};

        for (int dx : horizontalOffsets) {
            for (int dz : horizontalOffsets) {
                int bx = baseLoc.getBlockX() + dx;
                int bz = baseLoc.getBlockZ() + dz;
                int surfaceY = world.getHighestBlockYAt(bx, bz);
                int[] candidateY = {
                    Math.max(surfaceY + 15, baseLoc.getBlockY() + 30),
                    Math.max(surfaceY + 10, baseLoc.getBlockY() + 50),
                    Math.max(surfaceY + 25, baseLoc.getBlockY() + 70),
                    Math.max(surfaceY + 5, baseLoc.getBlockY() + 20)
                };

                for (int ty : candidateY) {
                    int targetY = Math.max(minY, Math.min(maxY, ty));
                    Location candidate = new Location(world, bx, targetY, bz);
                    if (!isCageOverlapping(candidate) && isCageAreaClear(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean isCageAreaClear(Location center) {
        if (center == null || center.getWorld() == null) return false;
        org.bukkit.World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = -4; z <= 4; z++) {
                    org.bukkit.block.Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (!b.getType().isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isCageOverlapping(Location candidate) {
        if (candidate == null || candidate.getWorld() == null) return true;
        for (Location existing : playerCages.values()) {
            if (candidate.getWorld().equals(existing.getWorld())) {
                if (Math.abs(candidate.getBlockX() - existing.getBlockX()) <= 8 &&
                    Math.abs(candidate.getBlockZ() - existing.getBlockZ()) <= 8 &&
                    Math.abs(candidate.getBlockY() - existing.getBlockY()) <= 8) {
                    return true;
                }
            }
        }
        return false;
    }

    public UUID getVictimForGuard(UUID guardId) {
        if (guardId == null) return null;
        for (Map.Entry<UUID, Set<UUID>> entry : playerGuards.entrySet()) {
            if (entry.getValue().contains(guardId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void checkGuardDeath(UUID playerUuid, UUID guardId) {
        if (playerUuid == null) return;
        Set<UUID> guards = playerGuards.get(playerUuid);
        if (guards != null && guards.remove(guardId)) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (guards.isEmpty()) {
                releasePlayer(playerUuid);
            } else if (player != null && player.isOnline()) {
                player.sendMessage(
                        SMPTools.getInstance().getMessageManager().getMessage("krampus.guard-defeated").replaceText(builder -> builder.matchLiteral("{remaining}").replacement(String.valueOf(guards.size()))));
            }
        }
    }

    public void checkGuardDeath(Player player, UUID guardId) {
        if (player == null) return;
        checkGuardDeath(player.getUniqueId(), guardId);
    }

    public void releasePlayer(Player player) {
        if (player == null) return;
        releasePlayer(player.getUniqueId());
    }

    public void releasePlayer(UUID uuid) {
        if (!kidnappedPlayers.containsKey(uuid) && !playerCages.containsKey(uuid))
            return;

        Location cageLoc = playerCages.remove(uuid);
        Location originalLoc = kidnappedPlayers.remove(uuid);
        despawnGuards(uuid);

        if (cageLoc != null) {
            removeCage(uuid, cageLoc);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && originalLoc != null) {
            player.teleport(originalLoc);
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("krampus.escaped"));
        }
    }

    private void despawnGuards(UUID uuid) {
        Set<UUID> guards = playerGuards.remove(uuid);
        if (guards != null) {
            for (UUID guardId : guards) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(guardId);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
    }

    private void removeCage(UUID uuid, Location center) {
        Map<Location, org.bukkit.block.BlockState> original = originalCageBlocks.remove(uuid);
        if (original != null) {
            for (org.bukkit.block.BlockState state : original.values()) {
                state.update(true, false);
            }
        }
        removeCageLocation(uuid);
    }

    public void cleanupAll() {
        for (UUID uuid : new HashSet<>(playerCages.keySet())) {
            releasePlayer(uuid);
        }
    }

    private void buildCage(UUID uuid, Location center) {
        if (center == null || center.getWorld() == null) return;
        Map<Location, org.bukkit.block.BlockState> snapshot = new HashMap<>();

        // Snapshot original block states (preserves TileEntity metadata & data)
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = -4; z <= 4; z++) {
                    Location loc = center.clone().add(x, y, z);
                    snapshot.put(loc, loc.getBlock().getState());
                }
            }
        }
        originalCageBlocks.put(uuid, snapshot);
        saveCageLocation(uuid, center);

        // 9x9 Cage (Radius 4)
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (x == -4 || x == 4 || z == -4 || z == 4 || y == 0 || y == 5) {
                        center.clone().add(x, y, z).getBlock().setType(Material.IRON_BARS);
                    } else {
                        center.clone().add(x, y, z).getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        // Floor
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                center.clone().add(x, 0, z).getBlock().setType(Material.BEDROCK);
            }
        }
    }

    public boolean isKidnapped(Player player) {
        return player != null && kidnappedPlayers.containsKey(player.getUniqueId());
    }
}

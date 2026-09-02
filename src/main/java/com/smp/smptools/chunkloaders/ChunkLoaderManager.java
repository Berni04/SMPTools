package com.smp.smptools.chunkloaders;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkLoaderManager {

    private final SMPTools plugin;
    private File chunkLoadersFile;
    private FileConfiguration chunkLoadersConfig;
    private final List<Location> activeChunkLoaders = new ArrayList<>();
    private static SMPTools staticPluginInstance;

    // Cached config values
    private static Material cachedMaterial;
    private static Component cachedName;
    private static List<Component> cachedLore;

    public ChunkLoaderManager(SMPTools plugin) {
        this.plugin = plugin;
        staticPluginInstance = plugin;
        if (plugin != null) {
            setupChunkLoadersConfig();
            loadChunkLoaders();
            cacheConfigValues();
        } else {
            this.chunkLoadersConfig = new YamlConfiguration();
        }
    }

    private void cacheConfigValues() {
        String materialStr = plugin.getConfig().getString("features.chunk-loaders.item.material", "BEACON");
        cachedMaterial = Material.matchMaterial(materialStr);
        if (cachedMaterial == null) {
            cachedMaterial = Material.BEACON;
        }

        cachedName = MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("features.chunk-loaders.item.name", "<gold>Chunk Loader</gold>"));

        cachedLore = plugin.getConfig().getStringList("features.chunk-loaders.item.lore").stream()
                .map(MiniMessage.miniMessage()::deserialize)
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);
    }

    private void setupChunkLoadersConfig() {
        chunkLoadersFile = new File(plugin.getDataFolder(), "chunkloaders.yml");
        if (!chunkLoadersFile.exists()) {
            try {
                chunkLoadersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create chunkloaders.yml file!");
            }
        }
        chunkLoadersConfig = YamlConfiguration.loadConfiguration(chunkLoadersFile);
    }

    private final Map<Location, UUID> loaderOwners = new ConcurrentHashMap<>();

    public void loadChunkLoaders() {
        activeChunkLoaders.clear();
        loaderOwners.clear();
        if (chunkLoadersConfig.contains("loaders")) {
            List<String> loaderStrings = chunkLoadersConfig.getStringList("loaders");
            for (String loaderString : loaderStrings) {
                try {
                    String[] parts = loaderString.split(";");
                    Location loc = deserializeLocation(loaderString);
                    if (loc != null) {
                        forceLoadChunk(loc);
                        activeChunkLoaders.add(loc);
                        if (parts.length >= 5) {
                            try {
                                loaderOwners.put(loc, UUID.fromString(parts[4]));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    } else {
                        plugin.getLogger().warning("Skipping invalid chunk loader entry: '" + loaderString + "'");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load chunk loader entry '" + loaderString + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Loaded " + activeChunkLoaders.size() + " chunk loaders.");
    }

    public void saveChunkLoaders() {
        List<String> loaderStrings = new ArrayList<>();
        for (Location loc : activeChunkLoaders) {
            loaderStrings.add(serializeLocation(loc));
        }
        chunkLoadersConfig.set("loaders", loaderStrings);
        try {
            com.smp.smptools.utils.AtomicFileWriter.save(chunkLoadersConfig, chunkLoadersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chunkloaders.yml file!");
        }
    }

    public boolean canPlaceChunkLoader(UUID owner, Player player) {
        int maxGlobal = plugin != null ? plugin.getConfig().getInt("features.chunk-loaders.max-global", 128) : 128;
        int maxPerPlayer = plugin != null ? plugin.getConfig().getInt("features.chunk-loaders.max-per-player", 8) : 8;

        if (activeChunkLoaders.size() >= maxGlobal) {
            if (player != null) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Global chunk loader limit reached (" + maxGlobal + ")!</red>"));
            }
            return false;
        }

        if (owner != null && player != null && !player.hasPermission("smptools.chunkloaders.admin")) {
            if (getPlayerLoaderCount(owner) >= maxPerPlayer) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have reached your chunk loader limit (" + maxPerPlayer + ")!</red>"));
                return false;
            }
        }
        return true;
    }

    public boolean addChunkLoader(Location loc, UUID owner, Player player) {
        if (!canPlaceChunkLoader(owner, player)) {
            return false;
        }

        if (!activeChunkLoaders.contains(loc)) {
            forceLoadChunk(loc);
            activeChunkLoaders.add(loc);
            if (owner != null) {
                loaderOwners.put(loc, owner);
            }
            saveChunkLoaders();
            return true;
        }
        return false;
    }

    public void addChunkLoader(Location loc) {
        addChunkLoader(loc, null, null);
    }

    public void removeChunkLoader(Location loc) {
        if (activeChunkLoaders.remove(loc)) {
            loaderOwners.remove(loc);
            unforceLoadChunk(loc);
            saveChunkLoaders();
        }
    }

    public boolean isChunkLoader(Location loc) {
        return activeChunkLoaders.contains(loc);
    }

    public UUID getOwner(Location loc) {
        return loaderOwners.get(loc);
    }

    public int getPlayerLoaderCount(UUID playerUuid) {
        if (playerUuid == null) return 0;
        int count = 0;
        for (UUID owner : loaderOwners.values()) {
            if (playerUuid.equals(owner)) count++;
        }
        return count;
    }

    private void forceLoadChunk(Location loc) {
        Chunk chunk = loc.getChunk();
        chunk.setForceLoaded(true);
        plugin.getLogger().info("Force loaded chunk: " + chunk.getX() + ", " + chunk.getZ() + " in world " + Objects.requireNonNull(chunk.getWorld()).getName());
    }

    private void unforceLoadChunk(Location loc) {
        Chunk chunk = loc.getChunk();
        chunk.setForceLoaded(false);
        plugin.getLogger().info("Unforce loaded chunk: " + chunk.getX() + ", " + chunk.getZ() + " in world " + Objects.requireNonNull(chunk.getWorld()).getName());
    }

    public void unloadAllChunks() {
        for (Location loc : activeChunkLoaders) {
            unforceLoadChunk(loc);
        }
        activeChunkLoaders.clear();
        loaderOwners.clear();
        plugin.getLogger().info("Unloaded all chunk loaders.");
    }

    private String serializeLocation(Location loc) {
        UUID owner = loaderOwners.get(loc);
        String base = Objects.requireNonNull(loc.getWorld()).getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
        return owner != null ? base + ";" + owner : base;
    }

    public static class ParsedLocation {
        public final String worldName;
        public final int x, y, z;

        public ParsedLocation(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static ParsedLocation parseCoordinates(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(";");
        if (parts.length != 4 && parts.length != 5) return null;
        if (parts.length == 5) {
            try {
                UUID.fromString(parts[4]);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new ParsedLocation(parts[0], x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    Location deserializeLocation(String s) {
        ParsedLocation parsed = parseCoordinates(s);
        if (parsed == null) {
            if (plugin != null && s != null && !s.isBlank()) {
                plugin.getLogger().warning("Failed to parse chunk loader coordinates in line '" + s + "'");
            }
            return null;
        }
        if (Bukkit.getServer() == null) return null;
        World world = Bukkit.getWorld(parsed.worldName);
        if (world == null) return null;
        if (parsed.y < world.getMinHeight() || parsed.y >= world.getMaxHeight()) {
            if (plugin != null) {
                plugin.getLogger().warning("Chunk loader Y coordinate out of world bounds (" + parsed.y + " not in [" + world.getMinHeight() + ".." + (world.getMaxHeight() - 1) + "]) in line '" + s + "'");
            }
            return null;
        }
        return new Location(world, parsed.x, parsed.y, parsed.z);
    }

    public static final org.bukkit.NamespacedKey CHUNK_LOADER_KEY = new org.bukkit.NamespacedKey("smptools", "chunk_loader");

    public static ItemStack getChunkLoaderItem() {
        if (cachedMaterial == null) {
            return new ItemStack(Material.BEACON);
        }

        ItemStack item = new ItemStack(cachedMaterial);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(cachedName);
        meta.lore(cachedLore);
        meta.getPersistentDataContainer().set(CHUNK_LOADER_KEY, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isChunkLoaderItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || cachedMaterial == null) {
            return false;
        }

        if (item.getType() != cachedMaterial) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(CHUNK_LOADER_KEY, org.bukkit.persistence.PersistentDataType.BYTE);
    }
}

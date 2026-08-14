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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public void loadChunkLoaders() {
        activeChunkLoaders.clear();
        if (chunkLoadersConfig.contains("loaders")) {
            List<String> loaderStrings = chunkLoadersConfig.getStringList("loaders");
            for (String loaderString : loaderStrings) {
                try {
                    Location loc = deserializeLocation(loaderString);
                    if (loc != null) {
                        activeChunkLoaders.add(loc);
                        forceLoadChunk(loc);
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
            chunkLoadersConfig.save(chunkLoadersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chunkloaders.yml file!");
        }
    }

    public void addChunkLoader(Location loc) {
        if (!activeChunkLoaders.contains(loc)) {
            activeChunkLoaders.add(loc);
            forceLoadChunk(loc);
            saveChunkLoaders();
        }
    }

    public void removeChunkLoader(Location loc) {
        if (activeChunkLoaders.remove(loc)) {
            unforceLoadChunk(loc);
            saveChunkLoaders();
        }
    }

    public boolean isChunkLoader(Location loc) {
        return activeChunkLoaders.contains(loc);
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
        plugin.getLogger().info("Unloaded all chunk loaders.");
    }

    private String serializeLocation(Location loc) {
        return Objects.requireNonNull(loc.getWorld()).getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    Location deserializeLocation(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(";");
        if (parts.length == 4) {
            if (Bukkit.getServer() == null) return null;
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            try {
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                return new Location(world, x, y, z);
            } catch (NumberFormatException e) {
                if (plugin != null) {
                    plugin.getLogger().warning("Failed to parse chunk loader coordinates in line '" + s + "': " + e.getMessage());
                }
                return null;
            }
        }
        return null;
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
        if (meta.getPersistentDataContainer().has(CHUNK_LOADER_KEY, org.bukkit.persistence.PersistentDataType.BYTE)) {
            return true;
        }

        return Objects.equals(meta.displayName(), cachedName) && Objects.equals(meta.lore(), cachedLore);
    }
}

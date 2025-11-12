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
    private static SMPTools staticPluginInstance; // To access config from static method

    public ChunkLoaderManager(SMPTools plugin) {
        this.plugin = plugin;
        staticPluginInstance = plugin; // Set the static instance
        setupChunkLoadersConfig();
        loadChunkLoaders();
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
                Location loc = deserializeLocation(loaderString);
                if (loc != null) {
                    activeChunkLoaders.add(loc);
                    forceLoadChunk(loc);
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

    private Location deserializeLocation(String s) {
        String[] parts = s.split(";");
        if (parts.length == 4) {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        }
        return null;
    }

    // Utility method to create the chunk loader item
    public static ItemStack getChunkLoaderItem() {
        if (staticPluginInstance == null) {
            // Fallback or error handling if plugin instance is not set
            return new ItemStack(Material.BEACON);
        }
        Material material = Material.matchMaterial(staticPluginInstance.getConfig().getString("features.chunk-loaders.item.material", "BEACON"));
        ItemStack item = new ItemStack(material != null ? material : Material.BEACON);
        ItemMeta meta = item.getItemMeta();

        Component name = MiniMessage.miniMessage().deserialize(staticPluginInstance.getConfig().getString("features.chunk-loaders.item.name", "<gold>Chunk Loader</gold>"));
        List<Component> lore = staticPluginInstance.getConfig().getStringList("features.chunk-loaders.item.lore").stream()
                .map(MiniMessage.miniMessage()::deserialize)
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);

        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Utility method to check if an item is a chunk loader item
    public static boolean isChunkLoaderItem(ItemStack item) {
        if (staticPluginInstance == null || item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        Material configuredMaterial = Material.matchMaterial(staticPluginInstance.getConfig().getString("features.chunk-loaders.item.material", "BEACON"));
        
        if (item.getType() != configuredMaterial) {
            return false;
        }

        Component configuredName = MiniMessage.miniMessage().deserialize(staticPluginInstance.getConfig().getString("features.chunk-loaders.item.name", "<gold>Chunk Loader</gold>"));
        List<Component> configuredLore = staticPluginInstance.getConfig().getStringList("features.chunk-loaders.item.lore").stream()
                .map(MiniMessage.miniMessage()::deserialize)
                .collect(java.util.ArrayList::new, java.util.ArrayList::add, java.util.ArrayList::addAll);

        return Objects.equals(meta.displayName(), configuredName) && Objects.equals(meta.lore(), configuredLore);
    }
}
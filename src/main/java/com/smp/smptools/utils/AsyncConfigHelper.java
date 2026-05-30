package com.smp.smptools.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utility class for saving configuration files asynchronously.
 * Creates a snapshot of the config before saving to prevent race conditions.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class AsyncConfigHelper {

    private AsyncConfigHelper() {
        // Prevent instantiation
    }

    /**
     * Saves a configuration file asynchronously on a separate thread.
     * Creates a snapshot of the config data to prevent race conditions
     * with main thread mutations.
     *
     * @param plugin the plugin instance (used for scheduling)
     * @param config the FileConfiguration to save
     * @param file the File to save to
     * @param name the name of the configuration (for error messages)
     */
    public static void saveConfigAsync(Plugin plugin, FileConfiguration config, File file, String name) {
        // Create a snapshot of the config data on the main thread
        Map<String, Object> dataSnapshot = new HashMap<>(config.getValues(true));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Create a new config from the snapshot
                FileConfiguration snapshot = new YamlConfiguration();
                for (Map.Entry<String, Object> entry : dataSnapshot.entrySet()) {
                    snapshot.set(entry.getKey(), entry.getValue());
                }
                snapshot.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + name + "!", e);
            }
        });
    }
}

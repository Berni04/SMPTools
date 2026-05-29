package com.smp.smptools.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Utility class for saving configuration files asynchronously.
 * This prevents blocking the main server thread during file I/O operations,
 * which helps reduce server lag.
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
     * This method schedules the save operation to run asynchronously,
     * preventing the main server thread from being blocked by file I/O.
     *
     * @param plugin the plugin instance (used for scheduling)
     * @param config the FileConfiguration to save
     * @param file the File to save to
     * @param name the name of the configuration (for error messages)
     */
    public static void saveConfigAsync(Plugin plugin, FileConfiguration config, File file, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save " + name + "!", e);
            }
        });
    }
}

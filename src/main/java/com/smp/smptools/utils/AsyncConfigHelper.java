package com.smp.smptools.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public final class AsyncConfigHelper {

    private AsyncConfigHelper() {
        // Prevent instantiation
    }

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

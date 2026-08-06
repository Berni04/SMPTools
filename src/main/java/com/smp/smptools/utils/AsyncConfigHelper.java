package com.smp.smptools.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Utility class for saving configuration files asynchronously.
 * Serializes config to String on main thread to prevent race conditions,
 * and executes writes on a single-threaded executor to guarantee FIFO order.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class AsyncConfigHelper {

    private static volatile ExecutorService saveExecutor;

    private AsyncConfigHelper() {
        // Prevent instantiation
    }

    private static synchronized ExecutorService getExecutor() {
        if (saveExecutor == null || saveExecutor.isShutdown()) {
            saveExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "SMPTools-ConfigSaver");
                thread.setDaemon(true);
                return thread;
            });
        }
        return saveExecutor;
    }

    /**
     * Saves a configuration file asynchronously on a single background thread.
     * Serializes the config to a String on the main thread to create a
     * deep snapshot, preventing race conditions with mutable collections.
     *
     * @param plugin the plugin instance (used for logging/enabled status)
     * @param config the FileConfiguration to save
     * @param file the File to save to
     * @param name the name of the configuration (for error messages)
     */
    public static void saveConfigAsync(Plugin plugin, FileConfiguration config, File file, String name) {
        // Serialize to string on main thread - this creates a deep snapshot
        String data = config.saveToString();

        try {
            getExecutor().submit(() -> {
                try {
                    Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    if (plugin != null) {
                        plugin.getLogger().log(Level.SEVERE, "Could not save " + name + "!", e);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            // Fallback synchronous write if executor is already shut down
            try {
                Files.write(file.toPath(), data.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                if (plugin != null) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save " + name + "!", ex);
                }
            }
        }
    }

    /**
     * Shuts down the save executor service, draining and awaiting queued write operations.
     */
    public static void shutdown() {
        ExecutorService executorToShutdown;
        synchronized (AsyncConfigHelper.class) {
            executorToShutdown = saveExecutor;
            saveExecutor = null;
        }
        if (executorToShutdown != null && !executorToShutdown.isShutdown()) {
            executorToShutdown.shutdown();
            try {
                if (!executorToShutdown.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorToShutdown.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorToShutdown.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}


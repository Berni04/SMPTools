package com.smp.smptools.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
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

    private static ExecutorService saveExecutor;
    private static ExecutorService drainingExecutor;
    private static boolean shuttingDown = false;

    private AsyncConfigHelper() {
        // Prevent instantiation
    }

    private static synchronized ExecutorService getExecutor() {
        waitForDraining();
        if (saveExecutor == null || saveExecutor.isShutdown()) {
            saveExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "SMPTools-ConfigSaver");
                thread.setDaemon(true);
                return thread;
            });
            shuttingDown = false;
        }
        return saveExecutor;
    }

    private static void waitForDraining() {
        ExecutorService draining;
        synchronized (AsyncConfigHelper.class) {
            draining = drainingExecutor;
        }
        if (draining != null) {
            awaitExecutorTermination(draining);
            synchronized (AsyncConfigHelper.class) {
                if (drainingExecutor == draining) {
                    drainingExecutor = null;
                }
            }
        }
    }

    private static void awaitExecutorTermination(ExecutorService executor) {
        if (executor != null && !executor.isTerminated()) {
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    List<Runnable> pending = executor.shutdownNow();
                    for (Runnable task : pending) {
                        try {
                            task.run();
                        } catch (Exception ignored) {}
                    }
                }
            } catch (InterruptedException e) {
                List<Runnable> pending = executor.shutdownNow();
                for (Runnable task : pending) {
                    try {
                        task.run();
                    } catch (Exception ignored) {}
                }
                Thread.currentThread().interrupt();
            }
        }
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
            // Wait for any draining executor to finish before fallback write to prevent out-of-order writes
            waitForDraining();
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
            shuttingDown = true;
            waitForDraining();
            executorToShutdown = saveExecutor;
            saveExecutor = null;
            drainingExecutor = executorToShutdown;
        }

        if (executorToShutdown != null) {
            if (!executorToShutdown.isShutdown()) {
                executorToShutdown.shutdown();
            }
            awaitExecutorTermination(executorToShutdown);
            synchronized (AsyncConfigHelper.class) {
                if (drainingExecutor == executorToShutdown) {
                    drainingExecutor = null;
                }
                shuttingDown = false;
            }
        } else {
            synchronized (AsyncConfigHelper.class) {
                shuttingDown = false;
            }
        }
    }
}


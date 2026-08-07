package com.smp.smptools.storage;

import com.smp.smptools.SMPTools;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class StorageShutdownTest {

    private SMPTools createDummyPlugin() {
        try {
            org.bukkit.UnsafeValues unsafeValuesProxy = (org.bukkit.UnsafeValues) Proxy.newProxyInstance(
                    org.bukkit.UnsafeValues.class.getClassLoader(),
                    new Class<?>[]{org.bukkit.UnsafeValues.class},
                    (proxy, method, args) -> {
                        if (method.getReturnType().equals(boolean.class)) return false;
                        if (method.getReturnType().equals(int.class)) return 0;
                        return null;
                    }
            );

            org.bukkit.Server serverProxy = (org.bukkit.Server) Proxy.newProxyInstance(
                    org.bukkit.Server.class.getClassLoader(),
                    new Class<?>[]{org.bukkit.Server.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("getLogger")) {
                            return Logger.getLogger("StorageShutdownTest");
                        }
                        if (method.getName().equals("getUnsafe")) {
                            return unsafeValuesProxy;
                        }
                        if (method.getReturnType().equals(boolean.class)) return false;
                        if (method.getReturnType().equals(int.class)) return 0;
                        return null;
                    }
            );

            if (org.bukkit.Bukkit.getServer() == null) {
                try {
                    Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
                    serverField.setAccessible(true);
                    serverField.set(null, serverProxy);
                } catch (Exception ignored) {}
            }

            JavaPluginLoader loader = new JavaPluginLoader(serverProxy);
            PluginDescriptionFile description = new PluginDescriptionFile("SMPTools", "1.0-SNAPSHOT", "com.smp.smptools.SMPTools");
            File dataFolder = new File("target/test-data");
            File pluginFile = new File("target/test-plugin.jar");

            Constructor<SMPTools> ctor = SMPTools.class.getDeclaredConstructor(
                    JavaPluginLoader.class, PluginDescriptionFile.class, File.class, File.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(loader, description, dataFolder, pluginFile);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create dummy plugin for testing", e);
        }
    }

    @Test
    public void testJdbcStorageProviderForcedShutdownExecutesPendingTasks() throws Exception {
        SMPTools plugin = createDummyPlugin();
        assertNotNull(plugin, "Plugin must not be null");
        JdbcStorageProvider provider = new JdbcStorageProvider(plugin, StorageType.SQLITE);

        Field field = JdbcStorageProvider.class.getDeclaredField("writeExecutor");
        field.setAccessible(true);
        ExecutorService executor = (ExecutorService) field.get(provider);

        CountDownLatch blockingLatch = new CountDownLatch(1);
        AtomicBoolean task1Executed = new AtomicBoolean(false);
        AtomicBoolean task2Executed = new AtomicBoolean(false);

        // Submit a blocking task first so the executor thread stays busy
        executor.submit(() -> {
            try {
                blockingLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Queue pending tasks that will be returned by shutdownNow()
        executor.submit(() -> task1Executed.set(true));
        executor.submit(() -> task2Executed.set(true));

        // Set interrupt flag on calling thread to trigger InterruptedException in awaitTermination instantly
        Thread.currentThread().interrupt();
        try {
            provider.shutdown();
        } finally {
            Thread.interrupted(); // clear interrupt status for test framework thread
            blockingLatch.countDown();
        }

        assertTrue(task1Executed.get(), "Pending JDBC write task 1 should have been executed synchronously on shutdown thread");
        assertTrue(task2Executed.get(), "Pending JDBC write task 2 should have been executed synchronously on shutdown thread");
    }

    @Test
    public void testMongoStorageProviderForcedShutdownExecutesPendingTasks() throws Exception {
        SMPTools plugin = createDummyPlugin();
        assertNotNull(plugin, "Plugin must not be null");
        MongoStorageProvider provider = new MongoStorageProvider(plugin);

        Field field = MongoStorageProvider.class.getDeclaredField("writeExecutor");
        field.setAccessible(true);
        ExecutorService executor = (ExecutorService) field.get(provider);

        CountDownLatch blockingLatch = new CountDownLatch(1);
        AtomicBoolean task1Executed = new AtomicBoolean(false);
        AtomicBoolean task2Executed = new AtomicBoolean(false);

        // Submit a blocking task first so the executor thread stays busy
        executor.submit(() -> {
            try {
                blockingLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Queue pending tasks that will be returned by shutdownNow()
        executor.submit(() -> task1Executed.set(true));
        executor.submit(() -> task2Executed.set(true));

        // Set interrupt flag on calling thread to trigger InterruptedException in awaitTermination instantly
        Thread.currentThread().interrupt();
        try {
            provider.shutdown();
        } finally {
            Thread.interrupted(); // clear interrupt status for test framework thread
            blockingLatch.countDown();
        }

        assertTrue(task1Executed.get(), "Pending Mongo write task 1 should have been executed synchronously on shutdown thread");
        assertTrue(task2Executed.get(), "Pending Mongo write task 2 should have been executed synchronously on shutdown thread");
    }
}

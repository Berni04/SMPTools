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
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            SMPTools plugin = (SMPTools) unsafe.allocateInstance(SMPTools.class);

            Field loggerField = JavaPlugin.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            loggerField.set(plugin, Logger.getLogger("StorageShutdownTest"));

            return plugin;
        } catch (Exception e) {
            return null;
        }
    }

    @Test
    public void testJdbcStorageProviderForcedShutdownExecutesPendingTasks() throws Exception {
        SMPTools plugin = createDummyPlugin();
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

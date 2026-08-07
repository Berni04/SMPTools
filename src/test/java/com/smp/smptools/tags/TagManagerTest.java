package com.smp.smptools.tags;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class TagManagerTest {

    @Test
    public void testGetStatFromCacheNormalization() {
        TagManager manager = new TagManager(null);

        Map<String, Long> cache = new HashMap<>();
        cache.put("playtime_minutes", 150L);
        cache.put("ores_mined.diamond", 25L);

        // Lookup with exact key
        assertEquals(150L, manager.getStatFromCache(cache, "playtime_minutes"));
        assertEquals(25L, manager.getStatFromCache(cache, "ores_mined.diamond"));

        // Lookup with dot instead of underscore
        assertEquals(150L, manager.getStatFromCache(cache, "playtime.minutes"));

        // Lookup with underscore instead of dot
        assertEquals(25L, manager.getStatFromCache(cache, "ores_mined_diamond"));
    }

    @Test
    public void testGetStatFromCacheNullAndMissingLookups() {
        TagManager manager = new TagManager(null);

        Map<String, Long> cache = new HashMap<>();
        cache.put("playtime_minutes", 100L);

        // Null cache lookup
        assertNull(manager.getStatFromCache(null, "playtime_minutes"));

        // Null key lookup
        assertNull(manager.getStatFromCache(cache, null));

        // Missing key lookup
        assertNull(manager.getStatFromCache(cache, "non_existent_stat"));
    }

    @Test
    public void testClearCachedStatsIncrementsVersion() throws Exception {
        TagManager manager = new TagManager(null);
        UUID uuid = UUID.randomUUID();

        java.lang.reflect.Field globalField = TagManager.class.getDeclaredField("globalCacheVersion");
        globalField.setAccessible(true);
        java.util.concurrent.atomic.AtomicInteger globalVer = (java.util.concurrent.atomic.AtomicInteger) globalField.get(manager);

        int initialGlobal = globalVer.get();
        manager.clearCachedStats(null);
        assertEquals(initialGlobal + 1, globalVer.get());

        java.lang.reflect.Field playerMapField = TagManager.class.getDeclaredField("playerCacheVersions");
        playerMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, java.util.concurrent.atomic.AtomicInteger> playerVersions = (Map<UUID, java.util.concurrent.atomic.AtomicInteger>) playerMapField.get(manager);

        manager.clearCachedStats(uuid);
        assertTrue(playerVersions.containsKey(uuid));
        assertTrue(playerVersions.get(uuid).get() > 0);
    }

    @Test
    public void testEvictPlayerCacheRemovesEntries() throws Exception {
        TagManager manager = new TagManager(null);
        UUID uuid = UUID.randomUUID();

        java.lang.reflect.Field statCacheField = TagManager.class.getDeclaredField("milestoneStatCache");
        statCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Map<String, Long>> statCache = (Map<UUID, Map<String, Long>>) statCacheField.get(manager);

        java.lang.reflect.Field playerMapField = TagManager.class.getDeclaredField("playerCacheVersions");
        playerMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, java.util.concurrent.atomic.AtomicInteger> playerVersions = (Map<UUID, java.util.concurrent.atomic.AtomicInteger>) playerMapField.get(manager);

        statCache.put(uuid, new HashMap<>());
        playerVersions.put(uuid, new java.util.concurrent.atomic.AtomicInteger(5));

        java.lang.reflect.Field loadingField = TagManager.class.getDeclaredField("loadingPlayers");
        loadingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Set<UUID> loadingPlayers = (java.util.Set<UUID>) loadingField.get(manager);

        // When loading is in flight, version is incremented and kept
        loadingPlayers.add(uuid);
        manager.evictPlayerCache(uuid);
        assertFalse(statCache.containsKey(uuid));
        assertTrue(playerVersions.containsKey(uuid));
        assertEquals(6, playerVersions.get(uuid).get());

        // When loading finishes, player version entry is cleaned up
        loadingPlayers.remove(uuid);
        manager.evictPlayerCache(uuid);
        assertFalse(playerVersions.containsKey(uuid));
    }

    @Test
    public void testLoadPlayerTitlesDoesNotMigrateOnFailedStorageRead() {
        boolean[] saveCalled = new boolean[]{false};

        com.smp.smptools.storage.StorageProvider providerProxy = (com.smp.smptools.storage.StorageProvider) Proxy.newProxyInstance(
                com.smp.smptools.storage.StorageProvider.class.getClassLoader(),
                new Class<?>[]{com.smp.smptools.storage.StorageProvider.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getAllPlayerTitles")) {
                        return null; // Transient storage read failure
                    }
                    if (method.getName().equals("savePlayerTitle")) {
                        saveCalled[0] = true;
                    }
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    return null;
                }
        );

        com.smp.smptools.SMPTools plugin = createDummyPlugin();
        com.smp.smptools.storage.StorageManager storageManager = new com.smp.smptools.storage.StorageManager(plugin, providerProxy);
        plugin.setStorageManager(storageManager);

        TagManager manager = new TagManager(plugin);
        manager.loadPlayerTitles();

        assertFalse(saveCalled[0], "Legacy migration and savePlayerTitle must NOT be triggered when storage read fails (returns null).");
    }

    private com.smp.smptools.SMPTools createDummyPlugin() {
        try {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            com.smp.smptools.SMPTools plugin = (com.smp.smptools.SMPTools) unsafe.allocateInstance(com.smp.smptools.SMPTools.class);

            java.lang.reflect.Field loggerField = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredField("logger");
            loggerField.setAccessible(true);
            loggerField.set(plugin, java.util.logging.Logger.getLogger("TagManagerTest"));

            return plugin;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create dummy plugin for testing", e);
        }
    }
}

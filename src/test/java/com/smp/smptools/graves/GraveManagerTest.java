package com.smp.smptools.graves;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GraveManagerTest {

    @Test
    public void testIsLegacyGrave() {
        GraveManager manager = new GraveManager(null);
        UUID owner = UUID.randomUUID();
        Location loc = new Location(null, 10, 64, 20);

        // Grave with timestamp in past
        Grave pastGrave = new Grave(owner, "Player1", loc, Collections.emptyList(), 1000L, "fall");
        assertTrue(manager.isLegacyGrave(pastGrave));

        // Future grave not in legacy list
        Grave futureGrave = new Grave(owner, "Player2", loc, Collections.emptyList(), System.currentTimeMillis() + 100_000L, "drown");
        assertFalse(manager.isLegacyGrave(futureGrave));

        // Explicitly marked legacy grave
        manager.markLegacyGraveForTest(loc);
        assertTrue(manager.isLegacyGrave(futureGrave));
    }

    @Test
    public void testIsMatchingGraveBlockLegacyAndPdc() {
        GraveManager manager = new GraveManager(null);
        UUID owner = UUID.randomUUID();
        Location graveLoc = new Location(null, 100, 65, -50);
        Location otherLoc = new Location(null, 200, 65, -50);

        Grave legacyGrave = new Grave(owner, "Player1", graveLoc, Collections.emptyList(), 1000L, "fall");
        Grave modernGrave = new Grave(owner, "Player1", graveLoc, Collections.emptyList(), System.currentTimeMillis() + 100_000L, "fall");

        // 1. Unmarked legacy skull at correct location with matching owner -> matches
        Block legacySkullAtLoc = createMockSkullBlock(Material.PLAYER_HEAD, graveLoc, null, owner);
        assertTrue(manager.isMatchingGraveBlock(legacySkullAtLoc, legacyGrave));

        // 1b. Unmarked legacy skull at correct location with different owner -> rejects
        UUID differentOwner = UUID.randomUUID();
        Block legacySkullDifferentOwner = createMockSkullBlock(Material.PLAYER_HEAD, graveLoc, null, differentOwner);
        assertFalse(manager.isMatchingGraveBlock(legacySkullDifferentOwner, legacyGrave),
                "Legacy skull with different owner UUID must be rejected");

        // 2. Unmarked legacy skull at wrong location -> rejects
        Block legacySkullAtWrongLoc = createMockSkullBlock(Material.PLAYER_HEAD, otherLoc, null, owner);
        assertFalse(manager.isMatchingGraveBlock(legacySkullAtWrongLoc, legacyGrave));

        // 3. Unmarked non-legacy modern skull -> rejects because PDC key is absent
        assertFalse(manager.isMatchingGraveBlock(legacySkullAtLoc, modernGrave));

        // 4. Marked modern skull with matching owner -> matches
        Block modernSkull = createMockSkullBlock(Material.PLAYER_HEAD, graveLoc, owner.toString(), owner);
        assertTrue(manager.isMatchingGraveBlock(modernSkull, modernGrave));

        // 5. Marked skull with mismatched owner -> rejects
        Block mismatchedSkull = createMockSkullBlock(Material.PLAYER_HEAD, graveLoc, UUID.randomUUID().toString(), owner);
        assertFalse(manager.isMatchingGraveBlock(mismatchedSkull, modernGrave));

        // 6. Non-skull block -> rejects
        Block stoneBlock = createMockBlock(Material.STONE, graveLoc);
        assertFalse(manager.isMatchingGraveBlock(stoneBlock, legacyGrave));
    }

    private Block createMockBlock(Material material, Location loc) {
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getType")) return material;
                    if (method.getName().equals("getLocation")) return loc;
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Block createMockSkullBlock(Material material, Location loc, String pdcOwner, UUID owningPlayerId) {
        PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
                PersistentDataContainer.class.getClassLoader(),
                new Class<?>[]{PersistentDataContainer.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("get")) {
                        return pdcOwner;
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        org.bukkit.OfflinePlayer offlinePlayer = (org.bukkit.OfflinePlayer) Proxy.newProxyInstance(
                org.bukkit.OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{org.bukkit.OfflinePlayer.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return owningPlayerId;
                    return defaultValue(method.getReturnType());
                }
        );

        Skull skullState = (Skull) Proxy.newProxyInstance(
                Skull.class.getClassLoader(),
                new Class<?>[]{Skull.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getPersistentDataContainer")) return pdc;
                    if (method.getName().equals("getOwningPlayer")) return offlinePlayer;
                    return defaultValue(method.getReturnType());
                }
        );

        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getType")) return material;
                    if (method.getName().equals("getLocation")) return loc;
                    if (method.getName().equals("getState")) return skullState;
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType.equals(boolean.class)) return false;
        if (returnType.equals(int.class)) return 0;
        if (returnType.equals(long.class)) return 0L;
        if (returnType.equals(double.class)) return 0.0;
        return null;
    }

    @Test
    public void testUnloadedWorldGraveSerialization() throws Exception {
        GraveManager manager = new GraveManager(null);
        java.io.File tempFile = java.io.File.createTempFile("graves_test", ".yml");
        tempFile.deleteOnExit();

        org.bukkit.configuration.file.YamlConfiguration config =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(tempFile);

        java.lang.reflect.Field fileField = GraveManager.class.getDeclaredField("gravesFile");
        fileField.setAccessible(true);
        fileField.set(manager, tempFile);

        java.lang.reflect.Field configField = GraveManager.class.getDeclaredField("gravesConfig");
        configField.setAccessible(true);
        configField.set(manager, config);

        java.lang.reflect.Field gravesMapField = GraveManager.class.getDeclaredField("graves");
        gravesMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<Location, Grave> gravesMap = (java.util.Map<Location, Grave>) gravesMapField.get(manager);

        // Location with null world (simulating unloaded world)
        Location unloadedLoc = new Location(null, 50, 70, -100);
        UUID owner = UUID.randomUUID();
        Grave grave = new Grave(owner, "TestPlayer", unloadedLoc, "custom_world", Collections.emptyList(), 5000L, "fall");
        gravesMap.put(unloadedLoc, grave);

        java.lang.reflect.Method saveMethod = GraveManager.class.getDeclaredMethod("saveGraves");
        saveMethod.setAccessible(true);
        boolean saved = (boolean) saveMethod.invoke(manager);
        assertTrue(saved, "saveGraves must succeed even when Location.getWorld() is null");

        // Verify loaded config contains stable coordinates and worldName
        org.bukkit.configuration.file.YamlConfiguration reloadedConfig =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(tempFile);
        java.util.List<java.util.Map<?, ?>> list = reloadedConfig.getMapList("graves");
        assertEquals(1, list.size());
        assertEquals("custom_world", list.get(0).get("worldName"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> locMap = (java.util.Map<String, Object>) list.get(0).get("location");
        assertNotNull(locMap);
        assertEquals("custom_world", locMap.get("world"));
        assertEquals(50.0, ((Number) locMap.get("x")).doubleValue());
    }
}

package com.smp.smptools.locks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class LockManagerTest {

    @Test
    public void testWorldUidKeyEncodingAndNoCollision() {
        World worldA = createWorldProxy("world.a");
        World world_A = createWorldProxy("world_a");
        Block blockA = createBlockProxy(worldA, 100, 64, -200);
        Block block_A = createBlockProxy(world_A, 100, 64, -200);

        LockManager manager = createTestLockManager();
        String keyA = manager.getBlockKey(blockA);
        String key_A = manager.getBlockKey(block_A);

        assertNotNull(keyA);
        assertNotNull(key_A);
        assertNotEquals(keyA, key_A, "World names 'world.a' and 'world_a' must not collide.");
        assertFalse(keyA.contains("."), "Key must not contain '.' so YamlConfiguration path resolution works properly.");
    }

    @Test
    public void testDoubleChestLockMigration() {
        World world = createWorldProxy("world");
        Block brokenBlock = createBlockProxy(world, 10, 64, 10);
        Block survivingBlock = createBlockProxy(world, 11, 64, 10);

        LockManager manager = createTestLockManager();
        java.util.UUID ownerUuid = java.util.UUID.randomUUID();
        java.util.UUID trustedUuid = java.util.UUID.randomUUID();

        String oldKey = manager.getBlockKey(brokenBlock);
        manager.getContainerOwners().put(oldKey, ownerUuid);

        manager.removeOrMigrateLock(brokenBlock, survivingBlock);

        assertFalse(manager.getContainerOwners().containsKey(oldKey), "Old key must be removed.");
        String newKey = manager.getLocationKey(survivingBlock.getLocation());
        assertEquals(ownerUuid, manager.getContainerOwners().get(newKey), "Lock owner must be migrated to surviving block location.");
    }

    private LockManager createTestLockManager() {
        return new LockManager(null) {
            @Override
            public void saveLocks() {
                // No-op for unit test without plugin data folder
            }
        };
    }

    private World createWorldProxy(String worldName) {
        java.util.UUID worldUid = java.util.UUID.nameUUIDFromBytes(worldName.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) {
                        return worldName;
                    }
                    if (method.getName().equals("getUID")) {
                        return worldUid;
                    }
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    if (method.getReturnType().equals(double.class)) return 0.0;
                    return null;
                }
        );
    }

    private Block createBlockProxy(World world, int x, int y, int z) {
        Location loc = new Location(world, x, y, z);
        return (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorld")) return world;
                    if (method.getName().equals("getX")) return x;
                    if (method.getName().equals("getY")) return y;
                    if (method.getName().equals("getZ")) return z;
                    if (method.getName().equals("getLocation")) {
                        if (args != null && args.length == 1 && args[0] instanceof Location targetLoc) {
                            targetLoc.setWorld(world);
                            targetLoc.setX(x);
                            targetLoc.setY(y);
                            targetLoc.setZ(z);
                            return targetLoc;
                        }
                        return loc;
                    }
                    if (method.getName().equals("getType")) return org.bukkit.Material.CHEST;
                    if (method.getName().equals("getState")) return null;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    if (method.getReturnType().equals(double.class)) return 0.0;
                    return null;
                }
        );
    }
}

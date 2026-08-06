package com.smp.smptools.locks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class LockManagerTest {

    @Test
    public void testWorldNameWithDotKeyEncoding() {
        World stubWorld = createWorldProxy("world.custom.nether");
        Block stubBlock = createBlockProxy(stubWorld, 100, 64, -200);

        LockManager manager = createTestLockManager();
        String key = manager.getBlockKey(stubBlock);

        assertNotNull(key);
        assertEquals("world_custom_nether:100:64:-200", key, "World name '.' should be replaced with '_' to avoid YamlConfiguration section nesting bugs.");
        assertFalse(key.contains("."), "Key must not contain '.' so YamlConfiguration path resolution works properly.");
    }

    @Test
    public void testNormalWorldKeyEncoding() {
        World stubWorld = createWorldProxy("world");
        Block stubBlock = createBlockProxy(stubWorld, 10, 20, 30);

        LockManager manager = createTestLockManager();
        String key = manager.getBlockKey(stubBlock);

        assertEquals("world:10:20:30", key);
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
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) {
                        return worldName;
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

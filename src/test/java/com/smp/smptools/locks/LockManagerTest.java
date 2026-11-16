package com.smp.smptools.locks;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.DoubleChestInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LockManagerTest {

    private final Map<String, Block> blockRegistry = new HashMap<>();
    private final Map<Block, org.bukkit.block.Chest> chestStateRegistry = new HashMap<>();

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

        createDoubleChestPair(brokenBlock, survivingBlock);

        LockManager manager = createTestLockManager();
        java.util.UUID ownerUuid = java.util.UUID.randomUUID();

        // 1. Verify double chest pairing key is generated for both halves
        String doubleChestKey = manager.getBlockKey(brokenBlock);
        assertNotNull(doubleChestKey);
        assertEquals(doubleChestKey, manager.getBlockKey(survivingBlock), "Both halves of double chest must yield identical key.");

        // 2. Lock the double chest using the double chest key
        manager.getContainerOwners().put(doubleChestKey, ownerUuid);
        assertTrue(manager.isLocked(brokenBlock), "Broken block half should be locked.");
        assertTrue(manager.isLocked(survivingBlock), "Surviving block half should be locked.");

        // 3. Verify getSurvivingDoubleChestHalf identifies the surviving half correctly
        Block survivingHalf = manager.getSurvivingDoubleChestHalf(brokenBlock);
        assertEquals(survivingBlock, survivingHalf, "Surviving half of double chest must be correctly resolved.");

        // 4. Simulate break & migration: broken block is removed, surviving block becomes single chest
        setSingleChest(survivingBlock);
        manager.removeOrMigrateLock(brokenBlock, survivingBlock);

        // 5. Assert old double chest key is removed and surviving block has lock migrated to its single chest key
        assertFalse(manager.getContainerOwners().containsKey(doubleChestKey), "Old double chest key must be removed.");
        String newKey = manager.getLocationKey(survivingBlock.getLocation());
        assertEquals(ownerUuid, manager.getContainerOwners().get(newKey), "Lock owner must be migrated to surviving block location.");
        assertTrue(manager.isLocked(survivingBlock), "Surviving block must remain locked under its single chest key.");
    }

    @Test
    public void testPairingTwoLockedSingleChestsMergesOwnersAndTrusted() {
        World world = createWorldProxy("world");
        Block leftBlock = createBlockProxy(world, 10, 64, 10);
        Block rightBlock = createBlockProxy(world, 11, 64, 10);

        setSingleChest(leftBlock);
        setSingleChest(rightBlock);

        LockManager manager = createTestLockManager();

        java.util.UUID ownerA = java.util.UUID.randomUUID();
        java.util.UUID trustedA = java.util.UUID.randomUUID();
        java.util.UUID ownerB = java.util.UUID.randomUUID();
        java.util.UUID trustedB = java.util.UUID.randomUUID();

        String leftKey = manager.getLocationKey(leftBlock.getLocation());
        String rightKey = manager.getLocationKey(rightBlock.getLocation());

        manager.getContainerOwners().put(leftKey, ownerA);
        manager.trustPlayer(leftBlock, createPlayerProxy(ownerA), createOfflinePlayerProxy(trustedA));

        manager.getContainerOwners().put(rightKey, ownerB);
        manager.trustPlayer(rightBlock, createPlayerProxy(ownerB), createOfflinePlayerProxy(trustedB));

        // Now pair into a double chest
        createDoubleChestPair(leftBlock, rightBlock);

        // getBlockKey must be a pure function and NOT mutate containerOwners on read path (CRIT-01)
        String doubleKey = manager.getBlockKey(leftBlock);
        assertNotNull(doubleKey);
        assertEquals(leftKey, doubleKey); // canonical key is leftKey (x=10)
        assertEquals(ownerA, manager.getContainerOwners().get(leftKey));
        assertEquals(ownerB, manager.getContainerOwners().get(rightKey));

        // Explicit migration moves rightKey to canonical key safely
        manager.migrateToDoubleChest(rightBlock, leftBlock);

        assertFalse(manager.getContainerOwners().containsKey(rightKey), "Right key must be removed after migration.");

        // Verify primary owner is ownerA
        assertEquals(ownerA, manager.getContainerOwners().get(doubleKey));

        // Verify both owners and trusted players retain access via canAccess
        assertTrue(manager.canAccess(leftBlock, createPlayerProxy(ownerA)), "Owner A must retain access.");
        assertTrue(manager.canAccess(leftBlock, createPlayerProxy(ownerB)), "Owner B must retain access (merged as trusted).");
        assertTrue(manager.canAccess(leftBlock, createPlayerProxy(trustedA)), "Trusted A must retain access.");
        assertTrue(manager.canAccess(leftBlock, createPlayerProxy(trustedB)), "Trusted B must retain access.");
    }

    @Test
    public void testDoubleChestUntrustRemovesFromBothHalves() {
        World world = createWorldProxy("world");
        Block leftBlock = createBlockProxy(world, 20, 64, 20);
        Block rightBlock = createBlockProxy(world, 21, 64, 20);

        setSingleChest(leftBlock);
        setSingleChest(rightBlock);

        LockManager manager = createTestLockManager();

        java.util.UUID owner = java.util.UUID.randomUUID();
        java.util.UUID untrustedUser = java.util.UUID.randomUUID();

        String leftKey = manager.getLocationKey(leftBlock.getLocation());
        String rightKey = manager.getLocationKey(rightBlock.getLocation());

        // Legacy state: two separate records for each half, both trusting untrustedUser
        manager.getContainerOwners().put(leftKey, owner);
        manager.trustPlayer(leftBlock, createPlayerProxy(owner), createOfflinePlayerProxy(untrustedUser));

        manager.getContainerOwners().put(rightKey, owner);
        manager.trustPlayer(rightBlock, createPlayerProxy(owner), createOfflinePlayerProxy(untrustedUser));

        createDoubleChestPair(leftBlock, rightBlock);

        // Untrust user
        boolean revoked = manager.untrustPlayer(leftBlock, createPlayerProxy(owner), createOfflinePlayerProxy(untrustedUser));
        assertTrue(revoked, "untrustPlayer should succeed.");

        // Simulate breaking left half; right half survives as single chest
        setSingleChest(rightBlock);
        manager.removeOrMigrateLock(leftBlock, rightBlock);

        // Verify surviving chest does not restore untrustedUser
        assertFalse(manager.canAccess(rightBlock, createPlayerProxy(untrustedUser)), "Revoked user must not retain access on surviving half.");
    }

    @Test
    public void testRemoveOrMigrateLockPreservesDisplacedOwner() {
        World world = createWorldProxy("world");
        Block leftBlock = createBlockProxy(world, 30, 64, 30);
        Block rightBlock = createBlockProxy(world, 31, 64, 30);

        setSingleChest(leftBlock);
        setSingleChest(rightBlock);

        LockManager manager = createTestLockManager();

        java.util.UUID ownerLeft = java.util.UUID.randomUUID();
        java.util.UUID ownerRight = java.util.UUID.randomUUID();

        String leftKey = manager.getLocationKey(leftBlock.getLocation());
        String rightKey = manager.getLocationKey(rightBlock.getLocation());

        manager.getContainerOwners().put(leftKey, ownerLeft);
        manager.getContainerOwners().put(rightKey, ownerRight);

        createDoubleChestPair(leftBlock, rightBlock);

        // Break left half; right half survives
        setSingleChest(rightBlock);
        manager.removeOrMigrateLock(leftBlock, rightBlock);

        // The surviving half should retain ownerRight as primary owner, but ownerLeft must be preserved in trusted
        assertTrue(manager.canAccess(rightBlock, createPlayerProxy(ownerRight)), "Surviving block owner must retain access.");
        assertTrue(manager.canAccess(rightBlock, createPlayerProxy(ownerLeft)), "Displaced owner must be preserved as trusted on surviving block.");
    }

    @Test
    public void testUntrustPlayerConsolidatesAndSavesEvenWhenTargetNotFound() {
        World world = createWorldProxy("world");
        Block leftBlock = createBlockProxy(world, 40, 64, 40);
        Block rightBlock = createBlockProxy(world, 41, 64, 40);

        setSingleChest(leftBlock);
        setSingleChest(rightBlock);

        final int[] saveCount = {0};
        LockManager manager = new LockManager(null) {
            @Override
            public void saveLocks() {
                saveCount[0]++;
            }
        };

        java.util.UUID owner = java.util.UUID.randomUUID();
        java.util.UUID unTargetedUser = java.util.UUID.randomUUID();

        String leftKey = manager.getLocationKey(leftBlock.getLocation());
        String rightKey = manager.getLocationKey(rightBlock.getLocation());

        manager.getContainerOwners().put(leftKey, owner);
        manager.getContainerOwners().put(rightKey, owner);

        createDoubleChestPair(leftBlock, rightBlock);

        // untrustPlayer for someone who is not trusted
        boolean removed = manager.untrustPlayer(leftBlock, createPlayerProxy(owner), createOfflinePlayerProxy(unTargetedUser));
        assertFalse(removed, "Should return false when user is not trusted.");
        assertFalse(manager.getContainerOwners().containsKey(rightKey), "The non-canonical half record must be consolidated.");
        assertEquals(owner, manager.getContainerOwners().get(leftKey), "The canonical record must retain the owner.");
        assertTrue(saveCount[0] > 0, "saveLocks must be called due to consolidation even when removed is false.");
    }

    @Test
    public void testUnauthorizedTrustUntrustDoesNotConsolidateOrSave() {
        World world = createWorldProxy("world");
        Block leftBlock = createBlockProxy(world, 50, 64, 50);
        Block rightBlock = createBlockProxy(world, 51, 64, 50);

        setSingleChest(leftBlock);
        setSingleChest(rightBlock);

        final int[] saveCount = {0};
        LockManager manager = new LockManager(null) {
            @Override
            public void saveLocks() {
                saveCount[0]++;
            }
        };

        java.util.UUID realOwner = java.util.UUID.randomUUID();
        java.util.UUID intruder = java.util.UUID.randomUUID();
        java.util.UUID target = java.util.UUID.randomUUID();

        String leftKey = manager.getLocationKey(leftBlock.getLocation());
        String rightKey = manager.getLocationKey(rightBlock.getLocation());

        manager.getContainerOwners().put(leftKey, realOwner);
        manager.getContainerOwners().put(rightKey, realOwner);

        createDoubleChestPair(leftBlock, rightBlock);

        // Intruder attempts to untrust
        boolean removed = manager.untrustPlayer(leftBlock, createPlayerProxy(intruder), createOfflinePlayerProxy(target));
        assertFalse(removed, "Unauthorized player must not untrust.");
        assertEquals(0, saveCount[0], "No save should occur for unauthorized untrust.");
        assertTrue(manager.getContainerOwners().containsKey(rightKey), "No consolidation should occur for unauthorized untrust.");

        // Intruder attempts to trust
        boolean trusted = manager.trustPlayer(leftBlock, createPlayerProxy(intruder), createOfflinePlayerProxy(target));
        assertFalse(trusted, "Unauthorized player must not trust.");
        assertEquals(0, saveCount[0], "No save should occur for unauthorized trust.");
        assertTrue(manager.getContainerOwners().containsKey(rightKey), "No consolidation should occur for unauthorized trust.");
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
                    if (method.getName().equals("getName")) return worldName;
                    if (method.getName().equals("getUID")) return worldUid;
                    if (method.getName().equals("getBlockAt")) {
                        String worldKey = (worldUid != null ? worldUid.toString() : worldName);
                        if (args != null && args.length == 3 && args[0] instanceof Integer x && args[1] instanceof Integer y && args[2] instanceof Integer z) {
                            return blockRegistry.get(worldKey + ":" + x + ":" + y + ":" + z);
                        }
                        if (args != null && args.length == 1 && args[0] instanceof Location loc) {
                            return blockRegistry.get(worldKey + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ());
                        }
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
        Block blockProxy = (Block) Proxy.newProxyInstance(
                Block.class.getClassLoader(),
                new Class<?>[]{Block.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("equals")) {
                        if (args == null || args.length != 1 || args[0] == null) return false;
                        if (proxy == args[0]) return true;
                        if (args[0] instanceof Block other) {
                            return x == other.getX() && y == other.getY() && z == other.getZ() && java.util.Objects.equals(world, other.getWorld());
                        }
                        return false;
                    }
                    if (method.getName().equals("hashCode")) return java.util.Objects.hash(world, x, y, z);
                    if (method.getName().equals("toString")) return "Block{" + x + "," + y + "," + z + "}";
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
                    if (method.getName().equals("getState")) return chestStateRegistry.get((Block) proxy);
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    if (method.getReturnType().equals(long.class)) return 0L;
                    if (method.getReturnType().equals(double.class)) return 0.0;
                    return null;
                }
        );
        String worldKey = (world.getUID() != null ? world.getUID().toString() : world.getName());
        blockRegistry.put(worldKey + ":" + x + ":" + y + ":" + z, blockProxy);
        return blockProxy;
    }

    private void createDoubleChestPair(Block leftBlock, Block rightBlock) {
        org.bukkit.inventory.Inventory leftInv = (org.bukkit.inventory.Inventory) Proxy.newProxyInstance(
                org.bukkit.inventory.Inventory.class.getClassLoader(),
                new Class<?>[]{org.bukkit.inventory.Inventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getHolder")) return chestStateRegistry.get(leftBlock);
                    return null;
                }
        );

        org.bukkit.inventory.Inventory rightInv = (org.bukkit.inventory.Inventory) Proxy.newProxyInstance(
                org.bukkit.inventory.Inventory.class.getClassLoader(),
                new Class<?>[]{org.bukkit.inventory.Inventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getHolder")) return chestStateRegistry.get(rightBlock);
                    return null;
                }
        );

        DoubleChestInventory doubleChestInv = (DoubleChestInventory) Proxy.newProxyInstance(
                DoubleChestInventory.class.getClassLoader(),
                new Class<?>[]{DoubleChestInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getLeftSide")) return leftInv;
                    if (method.getName().equals("getRightSide")) return rightInv;
                    return null;
                }
        );

        org.bukkit.block.Chest leftChestState = (org.bukkit.block.Chest) Proxy.newProxyInstance(
                org.bukkit.block.Chest.class.getClassLoader(),
                new Class<?>[]{org.bukkit.block.Chest.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getInventory")) return doubleChestInv;
                    if (method.getName().equals("getLocation")) return leftBlock.getLocation();
                    if (method.getName().equals("getBlock")) return leftBlock;
                    if (method.getName().equals("getType")) return org.bukkit.Material.CHEST;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    return null;
                }
        );

        org.bukkit.block.Chest rightChestState = (org.bukkit.block.Chest) Proxy.newProxyInstance(
                org.bukkit.block.Chest.class.getClassLoader(),
                new Class<?>[]{org.bukkit.block.Chest.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getInventory")) return doubleChestInv;
                    if (method.getName().equals("getLocation")) return rightBlock.getLocation();
                    if (method.getName().equals("getBlock")) return rightBlock;
                    if (method.getName().equals("getType")) return org.bukkit.Material.CHEST;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    return null;
                }
        );

        chestStateRegistry.put(leftBlock, leftChestState);
        chestStateRegistry.put(rightBlock, rightChestState);
    }

    private void setSingleChest(Block block) {
        org.bukkit.inventory.Inventory singleInv = (org.bukkit.inventory.Inventory) Proxy.newProxyInstance(
                org.bukkit.inventory.Inventory.class.getClassLoader(),
                new Class<?>[]{org.bukkit.inventory.Inventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getHolder")) return chestStateRegistry.get(block);
                    return null;
                }
        );

        org.bukkit.block.Chest singleChestState = (org.bukkit.block.Chest) Proxy.newProxyInstance(
                org.bukkit.block.Chest.class.getClassLoader(),
                new Class<?>[]{org.bukkit.block.Chest.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getInventory")) return singleInv;
                    if (method.getName().equals("getLocation")) return block.getLocation();
                    if (method.getName().equals("getBlock")) return block;
                    if (method.getName().equals("getType")) return org.bukkit.Material.CHEST;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    return null;
                }
        );

        chestStateRegistry.put(block, singleChestState);
    }

    private org.bukkit.entity.Player createPlayerProxy(java.util.UUID uuid) {
        return (org.bukkit.entity.Player) Proxy.newProxyInstance(
                org.bukkit.entity.Player.class.getClassLoader(),
                new Class<?>[]{org.bukkit.entity.Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getName().equals("hasPermission")) return false;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );
    }

    private org.bukkit.OfflinePlayer createOfflinePlayerProxy(java.util.UUID uuid) {
        return (org.bukkit.OfflinePlayer) Proxy.newProxyInstance(
                org.bukkit.OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{org.bukkit.OfflinePlayer.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return uuid;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    return null;
                }
        );
    }
}

package com.smp.smptools.artifacts;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ArtifactPouchOverflowTest {

    private static class DummyItemStack extends ItemStack {
        private final Material type;
        private final int amount;

        public DummyItemStack(Material type, int amount) {
            this.type = type;
            this.amount = amount;
        }

        @Override
        public Material getType() {
            return type;
        }

        @Override
        public int getAmount() {
            return amount;
        }

        @Override
        public ItemStack clone() {
            return new DummyItemStack(type, amount);
        }

        @Override
        public boolean hasItemMeta() {
            return false;
        }

        @Override
        public String toString() {
            return "DummyItemStack{" + type + "}";
        }
    }

    @Test
    public void testClaimPouchOverflowDeliversAndClears() {
        ArtifactManager manager = new ArtifactManager(null) {
            @Override
            public boolean savePouchDataWithResult() {
                return true;
            }
        };

        UUID playerUuid = UUID.randomUUID();
        ItemStack testItem = new DummyItemStack(Material.FEATHER, 1);

        try {
            java.lang.reflect.Field overflowField = ArtifactManager.class.getDeclaredField("pouchOverflow");
            overflowField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, List<ItemStack>> overflowMap = (Map<UUID, List<ItemStack>>) overflowField.get(manager);
            overflowMap.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(testItem);
        } catch (ReflectiveOperationException e) {
            fail("Failed to access pouchOverflow field: " + e.getMessage());
        }

        assertTrue(manager.hasPouchOverflow(playerUuid), "Should report overflow present");
        assertEquals(1, manager.getPouchOverflow(playerUuid).size());

        List<ItemStack> inventoryContents = new ArrayList<>();
        PlayerInventory invProxy = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addItem")) {
                        ItemStack[] items = (ItemStack[]) args[0];
                        inventoryContents.addAll(Arrays.asList(items));
                        return new HashMap<Integer, ItemStack>();
                    }
                    return null;
                }
        );

        Player playerProxy = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return playerUuid;
                    if (method.getName().equals("getInventory")) return invProxy;
                    return null;
                }
        );

        List<ItemStack> delivered = manager.claimPouchOverflow(playerProxy);
        assertNotNull(delivered);
        assertEquals(1, delivered.size(), "Should deliver 1 item");
        assertFalse(manager.hasPouchOverflow(playerUuid), "Overflow should be cleared after claim");
        assertEquals(0, manager.getPouchOverflow(playerUuid).size());
        assertEquals(1, inventoryContents.size());
    }

    @Test
    public void testClaimPouchOverflowAbortsAndPreservesOnSaveFailure() {
        ArtifactManager manager = new ArtifactManager(null) {
            @Override
            public boolean savePouchDataWithResult() {
                return false; // Simulate persistent storage failure
            }
        };

        UUID playerUuid = UUID.randomUUID();
        ItemStack testItem = new DummyItemStack(Material.FEATHER, 1);

        try {
            java.lang.reflect.Field overflowField = ArtifactManager.class.getDeclaredField("pouchOverflow");
            overflowField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, List<ItemStack>> overflowMap = (Map<UUID, List<ItemStack>>) overflowField.get(manager);
            overflowMap.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(testItem);
        } catch (ReflectiveOperationException e) {
            fail("Failed to access pouchOverflow field: " + e.getMessage());
        }

        assertTrue(manager.hasPouchOverflow(playerUuid));

        List<ItemStack> inventoryContents = new ArrayList<>();
        PlayerInventory invProxy = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addItem")) {
                        ItemStack[] items = (ItemStack[]) args[0];
                        inventoryContents.addAll(Arrays.asList(items));
                        return new HashMap<Integer, ItemStack>();
                    }
                    return null;
                }
        );

        Player playerProxy = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return playerUuid;
                    if (method.getName().equals("getInventory")) return invProxy;
                    return null;
                }
        );

        List<ItemStack> delivered = manager.claimPouchOverflow(playerProxy);
        assertNull(delivered, "Should return null when persistence fails");
        assertTrue(inventoryContents.isEmpty(), "Inventory must not receive items when persistence fails");
        assertTrue(manager.hasPouchOverflow(playerUuid), "Overflow entitlement must remain preserved in memory");
        assertEquals(1, manager.getPouchOverflow(playerUuid).size());
    }

    @Test
    public void testClaimPouchOverflowWithInvalidOrAirItemsReturnsEmptyListNotFailure() {
        ArtifactManager manager = new ArtifactManager(null) {
            @Override
            public boolean savePouchDataWithResult() {
                return true;
            }
        };

        UUID playerUuid = UUID.randomUUID();
        ItemStack airItem = new DummyItemStack(Material.AIR, 1);

        try {
            java.lang.reflect.Field overflowField = ArtifactManager.class.getDeclaredField("pouchOverflow");
            overflowField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, List<ItemStack>> overflowMap = (Map<UUID, List<ItemStack>>) overflowField.get(manager);
            overflowMap.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(airItem);
        } catch (ReflectiveOperationException e) {
            fail("Failed to access pouchOverflow field: " + e.getMessage());
        }

        assertTrue(manager.hasPouchOverflow(playerUuid));

        Player playerProxy = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return playerUuid;
                    return null;
                }
        );

        List<ItemStack> delivered = manager.claimPouchOverflow(playerProxy);
        assertNotNull(delivered, "Should return empty list on success even if entries were AIR");
        assertTrue(delivered.isEmpty(), "Delivered list must be empty");
        assertFalse(manager.hasPouchOverflow(playerUuid), "Overflow should be cleared of invalid/AIR entries");
    }

    @Test
    public void testVoidSavePouchDataDelegatesToSaveWithResult() {
        final boolean[] called = {false};
        ArtifactManager manager = new ArtifactManager(null) {
            @Override
            public boolean savePouchDataWithResult() {
                called[0] = true;
                return true;
            }
        };

        manager.savePouchData();
        assertTrue(called[0], "void savePouchData() must delegate to savePouchDataWithResult()");
    }

    @Test
    public void testClaimPouchOverflowExceptionRestoresUnprocessedItems() {
        ArtifactManager manager = new ArtifactManager(null) {
            @Override
            public boolean savePouchDataWithResult() {
                return true;
            }
        };

        UUID playerUuid = UUID.randomUUID();
        ItemStack item1 = new DummyItemStack(Material.FEATHER, 1);
        ItemStack item2 = new DummyItemStack(Material.EMERALD, 1);

        try {
            java.lang.reflect.Field overflowField = ArtifactManager.class.getDeclaredField("pouchOverflow");
            overflowField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, List<ItemStack>> overflowMap = (Map<UUID, List<ItemStack>>) overflowField.get(manager);
            overflowMap.computeIfAbsent(playerUuid, k -> new ArrayList<>()).addAll(List.of(item1, item2));
        } catch (ReflectiveOperationException e) {
            fail("Failed to access pouchOverflow field: " + e.getMessage());
        }

        PlayerInventory invProxy = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(),
                new Class<?>[]{PlayerInventory.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("addItem")) {
                        ItemStack[] items = (ItemStack[]) args[0];
                        if (items[0].getType() == Material.EMERALD) {
                            throw new IllegalStateException("Simulated delivery failure");
                        }
                        return new HashMap<Integer, ItemStack>();
                    }
                    return null;
                }
        );

        Player playerProxy = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return playerUuid;
                    if (method.getName().equals("getInventory")) return invProxy;
                    return null;
                }
        );

        assertThrows(IllegalStateException.class, () -> manager.claimPouchOverflow(playerProxy));

        // After failure, unreached item2 must remain preserved in pouchOverflow
        assertTrue(manager.hasPouchOverflow(playerUuid));
        List<ItemStack> remaining = manager.getPouchOverflow(playerUuid);
        assertEquals(1, remaining.size());
        assertEquals(Material.EMERALD, remaining.get(0).getType());
    }

    @Test
    public void testLoadPouchDataPreservesEquippedPouchesDuringReconciliation() throws Exception {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();

        UUID playerUuid = UUID.randomUUID();
        ItemStack equipped = new DummyItemStack(Material.FEATHER, 1);
        ItemStack deliveredItem = new DummyItemStack(Material.DIAMOND, 1);
        ItemStack undeliveredItem = new DummyItemStack(Material.EMERALD, 1);

        config.set("pouch." + playerUuid + ".11", equipped);
        config.set("pouch-pending-claim." + playerUuid + ".settled", true);
        config.set("pouch-pending-claim." + playerUuid + ".delivered", List.of(deliveredItem));
        config.set("pouch-pending-claim." + playerUuid + ".undelivered", List.of(undeliveredItem));

        boolean[] saveCalled = new boolean[]{false};
        ArtifactManager manager = new ArtifactManager(null) {
            @Override
            public ArtifactType getArtifactType(ItemStack item) {
                if (item != null && item.getType() == Material.FEATHER) {
                    return ArtifactType.FEATHER_GLIDER_RING;
                }
                return null;
            }

            @Override
            public boolean savePouchDataWithResult() {
                saveCalled[0] = true;
                // Verify that when reconciliation triggers a save, equippedPouchMap is already populated!
                try {
                    java.lang.reflect.Field field = ArtifactManager.class.getDeclaredField("equippedPouchMap");
                    field.setAccessible(true);
                    Map<?, ?> map = (Map<?, ?>) field.get(this);
                    assertFalse(map.isEmpty(), "equippedPouchMap must not be empty during reconciliation save");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return true;
            }
        };

        java.lang.reflect.Field configField = ArtifactManager.class.getDeclaredField("artifactsConfig");
        configField.setAccessible(true);
        configField.set(manager, config);

        java.lang.reflect.Method loadMethod = ArtifactManager.class.getDeclaredMethod("loadPouchData");
        loadMethod.setAccessible(true);
        loadMethod.invoke(manager);

        assertTrue(saveCalled[0], "Reconciliation must trigger a save to settle pending claims");

        // Equipped pouch items must be preserved!
        java.lang.reflect.Field equippedMapField = ArtifactManager.class.getDeclaredField("equippedPouchMap");
        equippedMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Map<Integer, ItemStack>> equippedMap = (Map<UUID, Map<Integer, ItemStack>>) equippedMapField.get(manager);
        assertTrue(equippedMap.containsKey(playerUuid));
        assertEquals(Material.FEATHER, equippedMap.get(playerUuid).get(11).getType());

        // Undelivered item must be reconciled into overflow
        assertTrue(manager.hasPouchOverflow(playerUuid));
        List<ItemStack> overflow = manager.getPouchOverflow(playerUuid);
        assertEquals(1, overflow.size());
        assertEquals(Material.EMERALD, overflow.get(0).getType());

        // Settled delivered item must not be restored to overflow (no duplicate items)
        for (ItemStack is : overflow) {
            assertNotEquals(Material.DIAMOND, is.getType(), "Delivered items must never be restored from settled claims");
        }

        // Pending claim section must be cleared from config
        assertNull(config.getConfigurationSection("pouch-pending-claim"));
    }
}

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
    }

    @Test
    public void testClaimPouchOverflowDeliversAndClears() {
        ArtifactManager manager = new ArtifactManager(null) {
            @Override
            public void savePouchData() {
                // No-op for test without plugin instance
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
        assertEquals(1, delivered.size(), "Should deliver 1 item");
        assertFalse(manager.hasPouchOverflow(playerUuid), "Overflow should be cleared after claim");
        assertEquals(0, manager.getPouchOverflow(playerUuid).size());
        assertEquals(1, inventoryContents.size());
    }
}

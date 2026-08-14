package com.smp.smptools.christmas;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SecretSantaManagerTest {

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
    }

    @Test
    public void testClaimGiftRemovesGiftAtomically() {
        SecretSantaManager manager = new SecretSantaManager(null);
        UUID santa = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();

        // Register players
        manager.registerPlayer(santa);
        manager.registerPlayer(recipient);
        assertTrue(manager.isRegistered(santa));
        assertTrue(manager.isRegistered(recipient));

        // Deposit gift
        ItemStack[] items = new ItemStack[]{
                new DummyItemStack(Material.DIAMOND, 5)
        };
        manager.depositGift(recipient, items);
        assertTrue(manager.hasGiftDeposited(recipient));

        // Claim once -> returns items
        ItemStack[] claimed = manager.claimGift(recipient);
        assertNotNull(claimed);
        assertEquals(1, claimed.length);
        assertEquals(Material.DIAMOND, claimed[0].getType());
        assertEquals(5, claimed[0].getAmount());

        // Gift is now gone
        assertFalse(manager.hasGiftDeposited(recipient));

        // Claim a second time -> returns null (dupe prevented)
        ItemStack[] secondClaim = manager.claimGift(recipient);
        assertNull(secondClaim);
    }
}

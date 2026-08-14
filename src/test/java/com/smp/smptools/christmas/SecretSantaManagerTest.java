package com.smp.smptools.christmas;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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

    @Test
    public void testDepositEmptyGiftDoesNotCreateDeposit() {
        SecretSantaManager manager = new SecretSantaManager(null);
        UUID recipient = UUID.randomUUID();

        manager.depositGift(recipient, new ItemStack[0]);
        assertFalse(manager.hasGiftDeposited(recipient));
        assertNull(manager.getGift(recipient));
        assertNull(manager.claimGift(recipient));
    }

    @Test
    public void testConcurrentClaimsRespectMutualExclusionAndPreventDuplication() throws Exception {
        // Verifies the synchronized claimGift contract: multiple threads attempting concurrent claims
        // are mutually excluded and exactly one caller receives the items while all others receive null.
        SecretSantaManager manager = new SecretSantaManager(null);
        UUID recipient = UUID.randomUUID();

        ItemStack[] items = new ItemStack[]{
                new DummyItemStack(Material.DIAMOND, 10)
        };
        manager.depositGift(recipient, items);
        assertTrue(manager.hasGiftDeposited(recipient));

        int threadCount = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger successfulClaims = new java.util.concurrent.atomic.AtomicInteger(0);

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    ItemStack[] claim = manager.claimGift(recipient);
                    if (claim != null && claim.length > 0) {
                        successfulClaims.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                }
            }));
        }

        latch.countDown();
        for (java.util.concurrent.Future<?> future : futures) {
            future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successfulClaims.get(), "Exactly 1 thread must successfully claim the gift under concurrent execution");
        assertFalse(manager.hasGiftDeposited(recipient));
    }
}

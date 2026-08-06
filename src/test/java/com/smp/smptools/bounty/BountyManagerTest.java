package com.smp.smptools.bounty;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class BountyManagerTest {

    @Test
    public void testBountyActiveState() {
        UUID placer = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Bounty bounty = new Bounty(null, placer, "Placer", target, "Target", new ArrayList<>(), System.currentTimeMillis(), null, 0L, false);

        assertTrue(bounty.isActive());
        assertFalse(bounty.isExpired());
        assertFalse(bounty.isClaimableByKiller(UUID.randomUUID()));

        UUID killer = UUID.randomUUID();
        bounty.setKillerUuid(killer);
        bounty.setKilledTimestamp(System.currentTimeMillis());

        assertFalse(bounty.isActive());
        assertTrue(bounty.isClaimableByKiller(killer));
        assertFalse(bounty.isRefundableToPlacer(placer));

        // Test 7-day expiration for refund
        bounty.setKilledTimestamp(System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000L));
        assertTrue(bounty.isExpired());
        assertFalse(bounty.isClaimableByKiller(killer));
        assertTrue(bounty.isRefundableToPlacer(placer));
    }

    @Test
    public void testUnmodifiableLists() {
        UUID placer = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        Bounty bounty = new Bounty(null, placer, "Placer", target, "Target", new ArrayList<>(), System.currentTimeMillis(), null, 0L, false);

        assertThrows(UnsupportedOperationException.class, () -> bounty.getItems().add(null));

        BountyManager manager = new BountyManager(null);
        assertThrows(UnsupportedOperationException.class, () -> manager.getBounties().add(bounty));
    }



    private static class DummyItemStack extends ItemStack {
        private final Material type;
        private int amount;

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
        public void setAmount(int amount) {
            this.amount = amount;
        }

        @Override
        public ItemStack clone() {
            return new DummyItemStack(type, amount);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof ItemStack other) {
                return this.type == other.getType() && this.amount == other.getAmount();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(type, amount);
        }

        @Override
        public String toString() {
            return "ItemStack{" + type + " x " + amount + "}";
        }
    }

    private static ItemStack createTestItemStack(Material type, int amount) {
        return new DummyItemStack(type, amount);
    }

    @Test
    public void testAutomaticExpiredRefund() throws Exception {
        BountyManager manager = new BountyManager(null);
        UUID placer = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID killer = UUID.randomUUID();

        List<ItemStack> items = List.of(
                createTestItemStack(Material.DIAMOND, 5),
                createTestItemStack(Material.GOLD_INGOT, 10)
        );

        Bounty expiredBounty = new Bounty(
                "test-id",
                placer,
                "Placer",
                target,
                "Target",
                items,
                System.currentTimeMillis() - 100000L,
                killer,
                System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000L),
                false
        );

        java.lang.reflect.Field field = BountyManager.class.getDeclaredField("bounties");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Bounty> list = (List<Bounty>) field.get(manager);
        list.add(expiredBounty);

        assertFalse(expiredBounty.isClaimed());
        assertTrue(expiredBounty.isExpired());

        manager.checkExpiredBounties();

        assertTrue(expiredBounty.isClaimed());

        Map<UUID, List<ItemStack>> pendingRefunds = manager.getPendingRefunds();
        assertTrue(pendingRefunds.containsKey(placer));
        List<ItemStack> placerRefunds = pendingRefunds.get(placer);
        assertNotNull(placerRefunds);
        assertEquals(2, placerRefunds.size());
        assertEquals(Material.DIAMOND, placerRefunds.get(0).getType());
        assertEquals(5, placerRefunds.get(0).getAmount());
        assertEquals(Material.GOLD_INGOT, placerRefunds.get(1).getType());
        assertEquals(10, placerRefunds.get(1).getAmount());
    }

    @Test
    public void testSaveExecutorIsDaemonThread() throws Exception {
        BountyManager manager = new BountyManager(null);
        java.lang.reflect.Field field = BountyManager.class.getDeclaredField("saveExecutor");
        field.setAccessible(true);
        java.util.concurrent.ExecutorService executor = (java.util.concurrent.ExecutorService) field.get(manager);

        java.util.concurrent.atomic.AtomicBoolean isDaemon = new java.util.concurrent.atomic.AtomicBoolean(false);
        executor.submit(() -> isDaemon.set(Thread.currentThread().isDaemon())).get();
        assertTrue(isDaemon.get(), "saveExecutor thread should be a daemon thread");
    }

    @Test
    public void testClaimBountyState() throws Exception {
        BountyManager manager = new BountyManager(null);
        UUID placer = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID killer = UUID.randomUUID();

        Bounty claimableBounty = new Bounty(
                "claim-test-id",
                placer,
                "Placer",
                target,
                "Target",
                new ArrayList<>(),
                System.currentTimeMillis() - 1000L,
                killer,
                System.currentTimeMillis(),
                false
        );

        java.lang.reflect.Field field = BountyManager.class.getDeclaredField("bounties");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Bounty> list = (List<Bounty>) field.get(manager);
        list.add(claimableBounty);

        assertFalse(claimableBounty.isClaimed());
        assertTrue(claimableBounty.isClaimableByKiller(killer));
    }

    @Test
    public void testCreateBountyReturnBool() {
        BountyManager manager = new BountyManager(null);
        assertFalse(manager.createBounty(null, null, null));
        assertFalse(manager.createBounty(null, null, List.of()));
    }
}

package com.smp.smptools.bounty;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    public void testAutomaticExpiredRefund() throws Exception {
        BountyManager manager = new BountyManager(null);
        UUID placer = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID killer = UUID.randomUUID();

        Bounty expiredBounty = new Bounty(
                "test-id",
                placer,
                "Placer",
                target,
                "Target",
                new ArrayList<>(),
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
}

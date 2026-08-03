package com.smp.smptools.bounty;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
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
}

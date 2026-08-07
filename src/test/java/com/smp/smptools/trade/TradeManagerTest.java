package com.smp.smptools.trade;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TradeManagerTest {

    @Test
    public void testSlotDefinitions() {
        assertEquals(16, TradeSession.P1_SLOTS.size());
        assertEquals(16, TradeSession.P2_SLOTS.size());
        assertEquals(16, TradeSession.P1_SLOTS_ORDERED.size());
        assertEquals(16, TradeSession.P2_SLOTS_ORDERED.size());
        assertTrue(TradeSession.P1_SLOTS.contains(0));
        assertTrue(TradeSession.P2_SLOTS.contains(5));
        assertTrue(TradeSession.DIVIDER_SLOTS.contains(4));
        assertFalse(TradeSession.DIVIDER_SLOTS.contains(TradeSession.CANCEL_SLOT));
        assertEquals(49, TradeSession.CANCEL_SLOT);
        assertEquals(0, TradeSession.P1_SLOTS_ORDERED.get(0));
        assertEquals(5, TradeSession.P2_SLOTS_ORDERED.get(0));
    }

    @Test
    public void testCleanupPendingRequests() {
        TradeManager manager = new TradeManager(null);
        java.util.UUID sender = java.util.UUID.randomUUID();
        java.util.UUID target = java.util.UUID.randomUUID();
        java.util.UUID sender2 = java.util.UUID.randomUUID();
        java.util.UUID target2 = java.util.UUID.randomUUID();

        manager.pendingRequests.put(target, new TradeManager.TradeRequest(sender, 1L));
        manager.pendingRequests.put(target2, new TradeManager.TradeRequest(sender2, 2L));

        assertEquals(2, manager.pendingRequests.size());

        // Target key cleanup removes request by key
        manager.cleanupPendingRequests(target);
        assertFalse(manager.pendingRequests.containsKey(target));
        assertEquals(1, manager.pendingRequests.size());

        // Sender key cleanup removes request where player is sender
        manager.cleanupPendingRequests(sender2);
        assertFalse(manager.pendingRequests.containsKey(target2));
        assertTrue(manager.pendingRequests.isEmpty());

        // Verify no exceptions on null or empty
        manager.cleanupPendingRequests(null);
    }
}

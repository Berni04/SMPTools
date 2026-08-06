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
        assertEquals(0, TradeSession.P1_SLOTS_ORDERED.get(0));
        assertEquals(5, TradeSession.P2_SLOTS_ORDERED.get(0));
    }
}

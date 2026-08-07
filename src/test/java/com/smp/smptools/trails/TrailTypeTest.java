package com.smp.smptools.trails;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TrailTypeTest {

    @Test
    public void testTrailTypeLookup() {
        assertEquals(TrailType.FLAME, TrailType.fromId("flame"));
        assertEquals(TrailType.HEART, TrailType.fromId("HEART"));
        assertEquals(TrailType.TOTEM, TrailType.fromId("totem"));
        assertNull(TrailType.fromId("invalid_trail"));
    }
}

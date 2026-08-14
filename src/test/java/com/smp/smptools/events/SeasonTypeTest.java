package com.smp.smptools.events;

import com.smp.smptools.events.seasonal.SeasonType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SeasonTypeTest {

    @Test
    public void testSeasonEnumValues() {
        assertNotNull(SeasonType.HALLOWEEN);
        assertNotNull(SeasonType.EASTER);
        assertNotNull(SeasonType.CHRISTMAS);
        assertNotNull(SeasonType.BLACK_FRIDAY);
        assertNotNull(SeasonType.SUMMER);
        assertNotNull(SeasonType.NONE);

        assertEquals("🎃", SeasonType.HALLOWEEN.getIconEmoji());
        assertEquals("🐣", SeasonType.EASTER.getIconEmoji());
        assertEquals("🎄", SeasonType.CHRISTMAS.getIconEmoji());
        assertEquals("🛍️", SeasonType.BLACK_FRIDAY.getIconEmoji());
        assertEquals("☀️", SeasonType.SUMMER.getIconEmoji());
    }

    @Test
    public void testMiniEventTypeConfigKeys() {
        for (com.smp.smptools.events.minievents.MiniEventType type : com.smp.smptools.events.minievents.MiniEventType.values()) {
            assertNotNull(type.getConfigKey());
            assertEquals(type.name().toLowerCase(), type.getConfigKey());
            assertNotNull(type.getDisplayName());
            assertNotNull(type.getIconEmoji());
            assertNotNull(type.getGuiMaterial());
            assertNotNull(type.getDescription());
        }
    }
}

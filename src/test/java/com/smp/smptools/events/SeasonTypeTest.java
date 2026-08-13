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
    public void testFormattedNames() {
        assertTrue(SeasonType.HALLOWEEN.getFormattedName().contains("Halloween"));
        assertTrue(SeasonType.EASTER.getFormattedName().contains("Easter"));
        assertTrue(SeasonType.SUMMER.getFormattedName().contains("Summer"));
    }
}

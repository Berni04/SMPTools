package com.smp.smptools.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void ticksPerSecond_isCorrect() {
        assertEquals(20L, Constants.TICKS_PER_SECOND);
    }

    @Test
    void ticksPerMinute_isCorrect() {
        assertEquals(1200L, Constants.TICKS_PER_MINUTE);
    }

    @Test
    void statsSaveInterval_isFiveMinutes() {
        assertEquals(6000L, Constants.STATS_SAVE_INTERVAL_TICKS);
    }

    @Test
    void autoSaveInterval_isFiveMinutes() {
        assertEquals(6000L, Constants.AUTO_SAVE_INTERVAL_TICKS);
    }

    @Test
    void tpaTimeout_isSixtySeconds() {
        assertEquals(60L, Constants.TPA_TIMEOUT_SECONDS);
        assertEquals(1200L, Constants.TPA_TIMEOUT_TICKS);
    }

    @Test
    void skillBaseExp_isPositive() {
        assertTrue(Constants.SKILL_BASE_EXP > 0);
    }

    @Test
    void skillGrowthRate_isGreaterThanOne() {
        assertTrue(Constants.SKILL_GROWTH_RATE > 1.0);
    }

    @Test
    void maxHomeNameLength_isPositive() {
        assertTrue(Constants.MAX_HOME_NAME_LENGTH > 0);
    }

    @Test
    void maxPlayerNameLength_isPositive() {
        assertTrue(Constants.MAX_PLAYER_NAME_LENGTH > 0);
    }

    @Test
    void homeNamePattern_isNotNull() {
        assertNotNull(Constants.HOME_NAME_PATTERN);
    }

    @Test
    void constructor_throwsException() {
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            var constructor = Constants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }
}

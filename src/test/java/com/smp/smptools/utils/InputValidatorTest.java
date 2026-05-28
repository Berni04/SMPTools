package com.smp.smptools.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void isValidHomeName_validName_returnsTrue() {
        assertTrue(InputValidator.isValidHomeName("myhome"));
        assertTrue(InputValidator.isValidHomeName("home-1"));
        assertTrue(InputValidator.isValidHomeName("base_main"));
    }

    @Test
    void isValidHomeName_null_returnsFalse() {
        assertFalse(InputValidator.isValidHomeName(null));
    }

    @Test
    void isValidHomeName_empty_returnsFalse() {
        assertFalse(InputValidator.isValidHomeName(""));
    }

    @Test
    void isValidHomeName_tooLong_returnsFalse() {
        String longName = "a".repeat(Constants.MAX_HOME_NAME_LENGTH + 1);
        assertFalse(InputValidator.isValidHomeName(longName));
    }

    @Test
    void isValidHomeName_exactlyMaxLength_returnsTrue() {
        String maxLengthName = "a".repeat(Constants.MAX_HOME_NAME_LENGTH);
        assertTrue(InputValidator.isValidHomeName(maxLengthName));
    }

    @Test
    void isValidHomeName_specialChars_returnsFalse() {
        assertFalse(InputValidator.isValidHomeName("home@name"));
        assertFalse(InputValidator.isValidHomeName("home name"));
        assertFalse(InputValidator.isValidHomeName("home#1"));
    }

    @Test
    void sanitizeString_null_returnsEmpty() {
        assertEquals("", InputValidator.sanitizeString(null));
    }

    @Test
    void sanitizeString_normalString_returnsSame() {
        assertEquals("hello world", InputValidator.sanitizeString("hello world"));
    }

    @Test
    void sanitizeString_specialChars_removesThem() {
        assertEquals("hello", InputValidator.sanitizeString("he@llo!"));
    }

    @Test
    void sanitizeString_allowedChars_keepsThem() {
        assertEquals("test-name_123", InputValidator.sanitizeString("test-name_123"));
    }

    @Test
    void isValidPlayerName_validName_returnsTrue() {
        assertTrue(InputValidator.isValidPlayerName("Player123"));
        assertTrue(InputValidator.isValidPlayerName("Test_User"));
    }

    @Test
    void isValidPlayerName_null_returnsFalse() {
        assertFalse(InputValidator.isValidPlayerName(null));
    }

    @Test
    void isValidPlayerName_empty_returnsFalse() {
        assertFalse(InputValidator.isValidPlayerName(""));
    }

    @Test
    void isValidPlayerName_tooLong_returnsFalse() {
        String longName = "a".repeat(Constants.MAX_PLAYER_NAME_LENGTH + 1);
        assertFalse(InputValidator.isValidPlayerName(longName));
    }

    @Test
    void isValidPlayerName_specialChars_returnsFalse() {
        assertFalse(InputValidator.isValidPlayerName("Player@123"));
    }
}

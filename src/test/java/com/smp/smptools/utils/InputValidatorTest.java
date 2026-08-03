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

    @Test
    void sanitizeMiniMessage_stripsLowercaseDangerousTags() {
        assertEquals("hi", InputValidator.sanitizeMiniMessage("<hover:show_text:'x'>hi"));
        assertEquals("hi", InputValidator.sanitizeMiniMessage("<click:run_command:/x>hi</click>"));
        assertEquals("hi", InputValidator.sanitizeMiniMessage("<insert:x>hi"));
    }

    @Test
    void sanitizeMiniMessage_stripsMixedCaseDangerousTags() {
        // Regression: regex was case-sensitive, so <HOVER>, <Hover> bypassed it.
        assertEquals("hi", InputValidator.sanitizeMiniMessage("<HOVER:show_text:'x'>hi"));
        assertEquals("hi", InputValidator.sanitizeMiniMessage("<Hover:show_text:'x'>hi</Hover>"));
        assertEquals("hi", InputValidator.sanitizeMiniMessage("<CLICK:run_command:/x>hi</click>"));
        assertEquals("hi", InputValidator.sanitizeMiniMessage("<TRANSLATABLE:x>hi</translatable>"));
    }

    @Test
    void sanitizeMiniMessage_preservesColorAndFormatTags() {
        assertEquals("<red>hi</red>", InputValidator.sanitizeMiniMessage("<red>hi</red>"));
        assertEquals("<bold>x</bold>", InputValidator.sanitizeMiniMessage("<bold>x</bold>"));
        assertEquals("<color:#FF00FF>x</color>", InputValidator.sanitizeMiniMessage("<color:#FF00FF>x</color>"));
    }

    @Test
    void sanitizeMiniMessage_null_returnsEmpty() {
        assertEquals("", InputValidator.sanitizeMiniMessage(null));
    }
}

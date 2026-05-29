package com.smp.smptools.utils;

/**
 * Utility class for validating and sanitizing user input.
 * Provides methods to check validity of home names, player names,
 * and sanitize strings to prevent injection attacks.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class InputValidator {

    private InputValidator() {
        // Prevent instantiation
    }

    /**
     * Validates whether a home name is acceptable.
     * A valid home name must:
     * <ul>
     *   <li>Not be null or empty</li>
     *   <li>Be at most {@link Constants#MAX_HOME_NAME_LENGTH} characters</li>
     *   <li>Contain only alphanumeric characters, underscores, and hyphens</li>
     * </ul>
     *
     * @param name the home name to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidHomeName(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= Constants.MAX_HOME_NAME_LENGTH
                && name.matches(Constants.HOME_NAME_PATTERN);
    }

    /**
     * Sanitizes a string by removing potentially dangerous characters.
     * Only allows alphanumeric characters, spaces, underscores, and hyphens.
     *
     * @param input the string to sanitize
     * @return the sanitized string, or empty string if input is null
     */
    public static String sanitizeString(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9 _-]", "");
    }

    /**
     * Validates whether a player name is acceptable.
     * A valid player name must:
     * <ul>
     *   <li>Not be null or empty</li>
     *   <li>Be at most {@link Constants#MAX_PLAYER_NAME_LENGTH} characters</li>
     *   <li>Contain only alphanumeric characters and underscores</li>
     * </ul>
     *
     * @param name the player name to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean isValidPlayerName(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= Constants.MAX_PLAYER_NAME_LENGTH
                && name.matches("[a-zA-Z0-9_]+");
    }
}

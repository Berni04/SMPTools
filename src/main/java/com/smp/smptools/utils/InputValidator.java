package com.smp.smptools.utils;

public final class InputValidator {

    private InputValidator() {
        // Prevent instantiation
    }

    public static boolean isValidHomeName(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= Constants.MAX_HOME_NAME_LENGTH
                && name.matches(Constants.HOME_NAME_PATTERN);
    }

    public static String sanitizeString(String input) {
        if (input == null) return "";
        return input.replaceAll("[^a-zA-Z0-9 _-]", "");
    }

    public static boolean isValidPlayerName(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= Constants.MAX_PLAYER_NAME_LENGTH
                && name.matches("[a-zA-Z0-9_]+");
    }
}

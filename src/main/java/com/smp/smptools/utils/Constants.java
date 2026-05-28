package com.smp.smptools.utils;

/**
 * Contains all constant values used throughout the SMPTools plugin.
 * This class centralizes magic numbers and configuration defaults
 * to improve maintainability and consistency.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class Constants {

    /** Number of game ticks in one second (20 ticks = 1 second) */
    public static final long TICKS_PER_SECOND = 20L;

    /** Number of game ticks in one minute (1200 ticks = 1 minute) */
    public static final long TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;

    /** Interval in ticks between automatic stats saves (5 minutes) */
    public static final long STATS_SAVE_INTERVAL_TICKS = TICKS_PER_MINUTE * 5;

    /** Interval in ticks between automatic data saves (5 minutes) */
    public static final long AUTO_SAVE_INTERVAL_TICKS = TICKS_PER_MINUTE * 5;

    /** TPA request timeout in seconds */
    public static final long TPA_TIMEOUT_SECONDS = 60;

    /** TPA request timeout in ticks */
    public static final long TPA_TIMEOUT_TICKS = TICKS_PER_SECOND * TPA_TIMEOUT_SECONDS;

    /** Base experience required for skill level 1 */
    public static final int SKILL_BASE_EXP = 100;

    /** Growth rate multiplier for each subsequent skill level */
    public static final double SKILL_GROWTH_RATE = 1.2;

    /** X-axis offset for sitting on stairs */
    public static final double SIT_OFFSET_X = 0.5;

    /** Y-axis offset for sitting on stairs */
    public static final double SIT_OFFSET_Y = 1.2;

    /** Z-axis offset for sitting on stairs */
    public static final double SIT_OFFSET_Z = 0.5;

    /** Maximum allowed length for home names */
    public static final int MAX_HOME_NAME_LENGTH = 32;

    /** Maximum allowed length for player names */
    public static final int MAX_PLAYER_NAME_LENGTH = 16;

    /** Regex pattern for valid home names (alphanumeric, underscore, hyphen) */
    public static final String HOME_NAME_PATTERN = "[a-zA-Z0-9_-]+";

    /** Radius for NPC cleanup operations */
    public static final double NPC_CLEANUP_RADIUS = 3.0;

    private Constants() {
        // Prevent instantiation
    }
}

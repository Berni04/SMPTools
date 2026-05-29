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

    // Troll durations
    /** Duration for blindness/nausea troll effects (10 seconds) */
    public static final long TROLL_BLINDNESS_DURATION_TICKS = TICKS_PER_SECOND * 10;
    
    /** Duration for levitation troll effect (5 seconds) */
    public static final long TROLL_LEVITATION_DURATION_TICKS = TICKS_PER_SECOND * 5;
    
    /** Duration for chat scramble troll effect (30 seconds) */
    public static final long TROLL_CHAT_SCRAMBLE_DURATION_MS = 30000;
    
    /** Duration for temporary block troll effect (3 seconds) */
    public static final long TROLL_TEMP_BLOCK_DURATION_TICKS = TICKS_PER_SECOND * 3;
    
    /** Radius for teleport troll effect */
    public static final int TROLL_TELEPORT_RADIUS = 10;
    
    /** Count for sound spam troll effect */
    public static final int TROLL_SOUND_SPAM_COUNT = 5;

    // Mission GUI
    /** Size for standard mission GUI */
    public static final int MISSION_GUI_SIZE = 27;
    
    /** Size for large mission GUI */
    public static final int MISSION_GUI_LARGE_SIZE = 54;
    
    /** Slot for back button in mission GUI */
    public static final int MISSION_BACK_BUTTON_SLOT = 49;
    
    /** Length of progress bar in mission GUI */
    public static final int MISSION_PROGRESS_BAR_LENGTH = 20;

    // Stats
    /** Minutes in one hour */
    public static final int MINUTES_PER_HOUR = 60;
    
    /** Minutes in one day */
    public static final int MINUTES_PER_DAY = 1440;

    // Treasure rarity thresholds
    /** Threshold for rare treasure (5%) */
    public static final double TREASURE_RARE_THRESHOLD = 0.05;
    
    /** Threshold for uncommon treasure (20%) */
    public static final double TREASURE_UNCOMMON_THRESHOLD = 0.25;

    private Constants() {
        // Prevent instantiation
    }
}

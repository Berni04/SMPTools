package com.smp.smptools.utils;

public final class Constants {

    // Timing constants
    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;
    public static final long STATS_SAVE_INTERVAL_TICKS = TICKS_PER_MINUTE * 5; // 5 minutes
    public static final long AUTO_SAVE_INTERVAL_TICKS = TICKS_PER_MINUTE * 5;
    public static final long TPA_TIMEOUT_SECONDS = 60;
    public static final long TPA_TIMEOUT_TICKS = TICKS_PER_SECOND * TPA_TIMEOUT_SECONDS;

    // Skill formula constants
    public static final int SKILL_BASE_EXP = 100;
    public static final double SKILL_GROWTH_RATE = 1.2;

    // Sitting constants
    public static final double SIT_OFFSET_X = 0.5;
    public static final double SIT_OFFSET_Y = 1.2;
    public static final double SIT_OFFSET_Z = 0.5;

    // Input validation limits
    public static final int MAX_HOME_NAME_LENGTH = 32;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final String HOME_NAME_PATTERN = "[a-zA-Z0-9_-]+";

    // NPC constants
    public static final double NPC_CLEANUP_RADIUS = 3.0;

    private Constants() {
        // Prevent instantiation
    }
}

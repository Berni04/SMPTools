package com.smp.smptools.utils;

/**
 * Centralized config path constants for SMPTools.
 * Prevents typos and makes config paths easy to update.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class ConfigPaths {

    // Feature toggles
    public static final String FEATURE_DAILY_REWARDS = "features.daily-rewards.enabled";
    public static final String FEATURE_CUSTOM_ENCHANTS = "features.custom-enchants.enabled";
    public static final String FEATURE_MMO_SKILLS = "features.mmo-skills.enabled";
    public static final String FEATURE_SIT_ON_STAIRS = "features.sit-on-stairs.enabled";
    public static final String FEATURE_PLAYER_GRAVES = "features.player-graves.enabled";
    public static final String FEATURE_IMAGE_TO_MAP = "features.image-to-map.enabled";
    public static final String FEATURE_MUSIC_PLAYER = "features.music-player.enabled";
    public static final String FEATURE_FUNNY_DEATH_MESSAGES = "features.funny-death-messages.enabled";
    public static final String FEATURE_RIDE = "features.ride.enabled";
    public static final String FEATURE_MEME_SOUNDS = "features.meme-sounds.enabled";
    public static final String FEATURE_SLEEP_VOTING = "features.sleep-voting.enabled";
    public static final String FEATURE_CHUNK_LOADERS = "features.chunk-loaders.enabled";
    public static final String FEATURE_ACCELERATED_GROWTH = "features.accelerated-growth.enabled";

    // Daily rewards
    public static final String DAILY_REWARDS_COOLDOWN_HOURS = "features.daily-rewards.cooldown-hours";
    public static final String DAILY_REWARDS_REWARDS = "features.daily-rewards.rewards";

    // MMO Skills - Mining
    public static final String MINING_ENABLED = "features.mmo-skills.mining.enabled";
    public static final String MINING_DOUBLE_DROP_CHANCE = "features.mmo-skills.mining.double-drop-chance";

    // MMO Skills - Woodcutting
    public static final String WOODCUTTING_ENABLED = "features.mmo-skills.woodcutting.enabled";
    public static final String WOODCUTTING_DOUBLE_DROP_CHANCE = "features.mmo-skills.woodcutting.double-drop-chance";

    // MMO Skills - Excavation
    public static final String EXCAVATION_ENABLED = "features.mmo-skills.excavation.enabled";
    public static final String EXCAVATION_DOUBLE_DROP_CHANCE = "features.mmo-skills.excavation.double-drop-chance";
    public static final String EXCAVATION_TREASURE_HUNTER_ENABLED = "features.mmo-skills.excavation.treasure-hunter.enabled";
    public static final String EXCAVATION_TREASURE_HUNTER_CHANCE = "features.mmo-skills.excavation.treasure-hunter.chance";
    public static final String EXCAVATION_TREASURE_HUNTER_LOOT_COMMON = "features.mmo-skills.excavation.treasure-hunter.loot.common";
    public static final String EXCAVATION_TREASURE_HUNTER_LOOT_UNCOMMON = "features.mmo-skills.excavation.treasure-hunter.loot.uncommon";
    public static final String EXCAVATION_TREASURE_HUNTER_LOOT_RARE = "features.mmo-skills.excavation.treasure-hunter.loot.rare";

    // MMO Skills - Combat
    public static final String COMBAT_ENABLED = "features.mmo-skills.combat.enabled";
    public static final String COMBAT_CRITICAL_STRIKE_ENABLED = "features.mmo-skills.combat.critical-strike.enabled";
    public static final String COMBAT_CRITICAL_STRIKE_CHANCE = "features.mmo-skills.combat.critical-strike.chance";
    public static final String COMBAT_CRITICAL_STRIKE_DAMAGE_MULTIPLIER = "features.mmo-skills.combat.critical-strike.damage-multiplier";

    // Custom enchants - Telekinesis
    public static final String TELEKINESIS_ENABLED = "features.custom-enchants.telekinesis.enabled";
    public static final String TELEKINESIS_DESCRIPTION = "features.custom-enchants.telekinesis.description";
    public static final String TELEKINESIS_APPLICABLE_ITEMS = "features.custom-enchants.telekinesis.applicable-items";

    // Custom enchants - Lumberjack
    public static final String LUMBERJACK_ENABLED = "features.custom-enchants.lumberjack.enabled";
    public static final String LUMBERJACK_DESCRIPTION = "features.custom-enchants.lumberjack.description";
    public static final String LUMBERJACK_APPLICABLE_ITEMS = "features.custom-enchants.lumberjack.applicable-items";

    // Music player
    public static final String MUSIC_PLAYER_BASE_URL = "features.music-player.base-url";

    // Meme sounds
    public static final String MEME_SOUNDS_RESOURCE_PACK_URL = "features.meme-sounds.resource-pack-url";
    public static final String MEME_SOUNDS_SOUNDS = "features.meme-sounds.sounds";

    // Chunk loaders
    public static final String CHUNK_LOADERS_ITEM_MATERIAL = "features.chunk-loaders.item.material";
    public static final String CHUNK_LOADERS_ITEM_NAME = "features.chunk-loaders.item.name";
    public static final String CHUNK_LOADERS_ITEM_LORE = "features.chunk-loaders.item.lore";

    // Accelerated growth
    public static final String ACCELERATED_GROWTH_MULTIPLIER = "features.accelerated-growth.multiplier";

    // Player data paths
    public static final String PLAYER_NAME_COLOR = "players.%s.name-color";
    public static final String PLAYER_PREFIX = "players.%s.prefix";

    // Homes
    public static final String HOMES_PATH = "homes.%s.%s";
    public static final String HOME_LIMITS = "home-limits";

    // Private vaults
    public static final String PRIVATE_VAULTS = "privatevaults.%s";
    public static final String PRIVATE_VAULT_SIZE = "private-vault-size";

    private ConfigPaths() {
        // Prevent instantiation
    }
}

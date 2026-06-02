package com.smp.smptools.utils;

import com.smp.smptools.SMPTools;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates the plugin configuration on startup.
 * Checks for missing keys, invalid values, and consistency issues.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public final class ConfigValidator {

    private final Logger logger;

    private ConfigValidator(Logger logger) {
        this.logger = logger;
    }

    /**
     * Validates the entire plugin configuration and reports issues.
     *
     * @param plugin the SMPTools plugin instance
     * @return the number of issues found
     */
    public static int validate(SMPTools plugin) {
        ConfigValidator validator = new ConfigValidator(plugin.getLogger());
        FileConfiguration config = plugin.getConfig();
        int issues = 0;

        issues += validator.validateFeatureToggles(config);
        issues += validator.validateDailyRewards(config);
        issues += validator.validatePrivateVault(config);
        issues += validator.validateHomeLimits(config);
        issues += validator.validateChunkLoaders(config);
        issues += validator.validateMemeSounds(config);
        issues += validator.validateMusicPlayer(config);

        if (issues == 0) {
            plugin.getLogger().info("Config validation passed - no issues found.");
        } else {
            plugin.getLogger().warning("Config validation found " + issues + " issue(s). Check above warnings.");
        }

        return issues;
    }

    private int validateFeatureToggles(FileConfiguration config) {
        int issues = 0;
        String[] featureToggles = {
            ConfigPaths.FEATURE_DAILY_REWARDS,
            ConfigPaths.FEATURE_CUSTOM_ENCHANTS,
            ConfigPaths.FEATURE_MMO_SKILLS,
            ConfigPaths.FEATURE_RIDE,
            ConfigPaths.FEATURE_MEME_SOUNDS,
            ConfigPaths.FEATURE_SLEEP_VOTING,
            ConfigPaths.FEATURE_CHUNK_LOADERS,
            ConfigPaths.FEATURE_ACCELERATED_GROWTH,
            ConfigPaths.FEATURE_IMAGE_TO_MAP,
            ConfigPaths.FEATURE_MUSIC_PLAYER,
            ConfigPaths.FEATURE_FUNNY_DEATH_MESSAGES,
            ConfigPaths.FEATURE_PLAYER_GRAVES,
            ConfigPaths.FEATURE_SIT_ON_STAIRS
        };

        for (String path : featureToggles) {
            if (!config.contains(path)) {
                logger.warning("Missing config key: " + path);
                issues++;
            } else if (!config.isBoolean(path)) {
                logger.warning("Config key should be a boolean: " + path);
                issues++;
            }
        }
        return issues;
    }

    private int validateDailyRewards(FileConfiguration config) {
        int issues = 0;
        if (config.getBoolean(ConfigPaths.FEATURE_DAILY_REWARDS, false)) {
            if (!config.contains(ConfigPaths.DAILY_REWARDS_COOLDOWN_HOURS)) {
                logger.warning("Missing config key: " + ConfigPaths.DAILY_REWARDS_COOLDOWN_HOURS);
                issues++;
            } else {
                long cooldown = config.getLong(ConfigPaths.DAILY_REWARDS_COOLDOWN_HOURS);
                if (cooldown <= 0) {
                    logger.warning("Daily rewards cooldown must be positive: " + ConfigPaths.DAILY_REWARDS_COOLDOWN_HOURS + " = " + cooldown);
                    issues++;
                }
            }
            if (!config.contains(ConfigPaths.DAILY_REWARDS_REWARDS)) {
                logger.warning("Missing config key: " + ConfigPaths.DAILY_REWARDS_REWARDS);
                issues++;
            } else if (config.getStringList(ConfigPaths.DAILY_REWARDS_REWARDS).isEmpty()) {
                logger.warning("Daily rewards list is empty: " + ConfigPaths.DAILY_REWARDS_REWARDS);
                issues++;
            }
        }
        return issues;
    }

    private int validatePrivateVault(FileConfiguration config) {
        int issues = 0;
        if (config.contains(ConfigPaths.PRIVATE_VAULT_SIZE)) {
            int size = config.getInt(ConfigPaths.PRIVATE_VAULT_SIZE, 54);
            if (size < 9 || size > 54 || size % 9 != 0) {
                logger.warning("Invalid " + ConfigPaths.PRIVATE_VAULT_SIZE + ": " + size + ". Must be 9, 18, 27, 36, 45, or 54.");
                issues++;
            }
        }
        return issues;
    }

    private int validateHomeLimits(FileConfiguration config) {
        int issues = 0;
        ConfigurationSection limitsSection = config.getConfigurationSection(ConfigPaths.HOME_LIMITS);
        if (limitsSection != null) {
            for (String key : limitsSection.getKeys(false)) {
                int limit = limitsSection.getInt(key);
                if (limit < 1) {
                    logger.warning("Invalid home limit for '" + key + "': " + limit + ". Must be at least 1.");
                    issues++;
                }
            }
        }
        return issues;
    }

    private int validateChunkLoaders(FileConfiguration config) {
        int issues = 0;
        if (config.getBoolean(ConfigPaths.FEATURE_CHUNK_LOADERS, false)) {
            String material = config.getString(ConfigPaths.CHUNK_LOADERS_ITEM_MATERIAL);
            if (material == null || material.isEmpty()) {
                logger.warning("Missing chunk loader item material: " + ConfigPaths.CHUNK_LOADERS_ITEM_MATERIAL);
                issues++;
            }
        }
        return issues;
    }

    private int validateMemeSounds(FileConfiguration config) {
        int issues = 0;
        if (config.getBoolean(ConfigPaths.FEATURE_MEME_SOUNDS, false)) {
            if (!config.contains(ConfigPaths.MEME_SOUNDS_SOUNDS)) {
                logger.warning("Missing config key: " + ConfigPaths.MEME_SOUNDS_SOUNDS);
                issues++;
            }
        }
        return issues;
    }

    private int validateMusicPlayer(FileConfiguration config) {
        int issues = 0;
        if (config.getBoolean(ConfigPaths.FEATURE_MUSIC_PLAYER, false)) {
            String baseUrl = config.getString(ConfigPaths.MUSIC_PLAYER_BASE_URL);
            if (baseUrl == null || baseUrl.isEmpty()) {
                logger.warning("Missing music player base URL: " + ConfigPaths.MUSIC_PLAYER_BASE_URL);
                issues++;
            }
        }
        return issues;
    }
}

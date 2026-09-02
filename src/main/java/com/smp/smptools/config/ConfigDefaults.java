package com.smp.smptools.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ConfigDefaults {

    private ConfigDefaults() {
        // Prevent instantiation
    }

    public static void applyDefaults(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();

        // Feature toggles
        config.addDefault("features.fly.enabled", true);
        config.addDefault("features.private-vault.enabled", true);
        config.addDefault("features.homes.enabled", true);
        config.addDefault("features.tpa.enabled", true);
        config.addDefault("features.msg.enabled", true);
        config.addDefault("features.tags.enabled", true);
        config.addDefault("features.prefix-color.enabled", true);
        config.addDefault("features.item-rename.enabled", true);
        config.addDefault("features.invsee.enabled", true);
        config.addDefault("features.troll.enabled", true);
        config.addDefault("features.sudo.enabled", true);
        config.addDefault("features.npcs.enabled", true);
        config.addDefault("features.leaderboard.enabled", true);
        config.addDefault("features.stats.enabled", true);
        config.addDefault("features.tab-health.enabled", true);
        config.addDefault("features.advancements.enabled", true);
        config.addDefault("features.elytra-trail.enabled", true);
        config.addDefault("features.daily-rewards.enabled", true);
        config.addDefault("features.custom-enchants.enabled", true);
        config.addDefault("features.mmo-skills.enabled", true);
        config.addDefault("features.missions.enabled", true);
        config.addDefault("features.christmas.enabled", true);
        config.addDefault("features.blackfriday.enabled", true);
        config.addDefault("features.afk.enabled", true);
        config.addDefault("features.afk.timeout-minutes", 30);
        config.addDefault("features.afk.auto-vote-sleep", false);

        // One-time upgrade migration for 1.1.2: harden default to false for existing installations
        if (!config.contains("migrations.v1_1_2_afk_sleep_default")) {
            config.set("features.afk.auto-vote-sleep", false);
            config.set("migrations.v1_1_2_afk_sleep_default", true);
        }
        config.addDefault("features.remote-trade.enabled", true);
        config.addDefault("features.remote-trade.request-timeout-seconds", 60);
        config.addDefault("features.remote-trade.session-timeout-seconds", 60);
        config.addDefault("features.trails.enabled", true);
        config.addDefault("features.bounties.enabled", true);
        config.addDefault("features.container-locks.enabled", true);
        config.addDefault("features.events.enabled", true);
        config.addDefault("features.artifacts.enabled", true);
        config.addDefault("features.seasonal.enabled", true);

        // Daily Rewards
        config.addDefault("features.daily-rewards.cooldown-hours", 22);
        List<String> defaultRewards = new ArrayList<>();
        defaultRewards.add("eco give %player% 100");
        defaultRewards.add("item:diamond 5");
        config.addDefault("features.daily-rewards.rewards", defaultRewards);

        // MMO Skills
        applyMMOSkillsDefaults(config);

        // Sit on Stairs
        config.addDefault("features.sit-on-stairs.enabled", true);

        // Player Graves
        config.addDefault("features.player-graves.enabled", true);

        // Custom Enchants
        applyCustomEnchantsDefaults(config);

        // Image to Map
        config.addDefault("features.image-to-map.enabled", true);

        // Music Player
        config.addDefault("features.music-player.enabled", true);
        config.addDefault("features.music-player.base-url",
                "https://raw.githubusercontent.com/YourUser/YourRepo/main/");

        // Funny Death Messages
        config.addDefault("features.funny-death-messages.enabled", true);

        // Ride Command
        config.addDefault("features.ride.enabled", true);

        // Meme Sounds
        applyMemeSoundsDefaults(config);

        // Sleep Voting
        config.addDefault("features.sleep-voting.enabled", true);

        // Chunk Loaders
        applyChunkLoadersDefaults(config);

        // Storage settings
        applyStorageDefaults(config);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private static void applyStorageDefaults(FileConfiguration config) {
        config.addDefault("storage.type", "FLATFILE");
        config.addDefault("storage.sqlite.file", "smptools.db");

        config.addDefault("storage.mysql.host", "localhost");
        config.addDefault("storage.mysql.port", 3306);
        config.addDefault("storage.mysql.database", "smptools");
        config.addDefault("storage.mysql.username", "root");
        config.addDefault("storage.mysql.password", "");
        config.addDefault("storage.mysql.pool-size", 10);
        config.addDefault("storage.mysql.use-ssl", false);

        config.addDefault("storage.mariadb.host", "localhost");
        config.addDefault("storage.mariadb.port", 3306);
        config.addDefault("storage.mariadb.database", "smptools");
        config.addDefault("storage.mariadb.username", "root");
        config.addDefault("storage.mariadb.password", "");
        config.addDefault("storage.mariadb.pool-size", 10);
        config.addDefault("storage.mariadb.use-ssl", false);

        config.addDefault("storage.mongodb.uri", "mongodb://localhost:27017");
        config.addDefault("storage.mongodb.database", "smptools");
        config.addDefault("storage.mongodb.collection-prefix", "smptools_");
    }

    private static void applyMMOSkillsDefaults(FileConfiguration config) {
        // Mining
        config.addDefault("features.mmo-skills.mining.enabled", true);
        config.addDefault("features.mmo-skills.mining.double-drop-chance", "0.0015 * level");

        // Woodcutting
        config.addDefault("features.mmo-skills.woodcutting.enabled", true);
        config.addDefault("features.mmo-skills.woodcutting.double-drop-chance", "0.0015 * level");

        // Excavation
        config.addDefault("features.mmo-skills.excavation.enabled", true);
        config.addDefault("features.mmo-skills.excavation.double-drop-chance", "0.0015 * level");

        // Treasure Hunter
        config.addDefault("features.mmo-skills.excavation.treasure-hunter.enabled", true);
        config.addDefault("features.mmo-skills.excavation.treasure-hunter.chance", "0.0005 * level");
        config.addDefault("features.mmo-skills.excavation.treasure-hunter.loot.common",
                List.of("IRON_NUGGET 1", "GOLD_NUGGET 1"));
        config.addDefault("features.mmo-skills.excavation.treasure-hunter.loot.uncommon",
                List.of("GLOWSTONE_DUST 2", "QUARTZ 1"));
        config.addDefault("features.mmo-skills.excavation.treasure-hunter.loot.rare",
                List.of("DIAMOND 1", "NAME_TAG 1"));

        // Combat
        config.addDefault("features.mmo-skills.combat.enabled", true);
        config.addDefault("features.mmo-skills.combat.critical-strike.enabled", true);
        config.addDefault("features.mmo-skills.combat.critical-strike.chance", "0.002 * level");
        config.addDefault("features.mmo-skills.combat.critical-strike.damage-multiplier", "1.0 + (level / 10.0)");
    }

    private static void applyCustomEnchantsDefaults(FileConfiguration config) {
        // Telekinesis
        config.addDefault("features.custom-enchants.telekinesis.enabled", true);
        config.addDefault("features.custom-enchants.telekinesis.description",
                "Automatically sends block drops to your inventory.");
        List<String> telekinesisApplicable = new ArrayList<>();
        telekinesisApplicable.add("PICKAXE");
        telekinesisApplicable.add("AXE");
        telekinesisApplicable.add("SHOVEL");
        telekinesisApplicable.add("HOE");
        config.addDefault("features.custom-enchants.telekinesis.applicable-items", telekinesisApplicable);

        // Lumberjack
        config.addDefault("features.custom-enchants.lumberjack.enabled", true);
        config.addDefault("features.custom-enchants.lumberjack.description", "Breaks an entire tree at once.");
        List<String> lumberjackApplicable = new ArrayList<>();
        lumberjackApplicable.add("AXE");
        config.addDefault("features.custom-enchants.lumberjack.applicable-items", lumberjackApplicable);
    }

    private static void applyMemeSoundsDefaults(FileConfiguration config) {
        config.addDefault("features.meme-sounds.enabled", true);
        config.addDefault("features.meme-sounds.resource-pack-url", "YOUR_RESOURCE_PACK_URL_HERE");
        if (!config.contains("features.meme-sounds.sounds")) {
            config.set("features.meme-sounds.sounds.vine_boom", "custom.vine_boom");
            config.set("features.meme-sounds.sounds.goofy_yell", "custom.goofy_yell");
            config.set("features.meme-sounds.sounds.crickets", "custom.crickets");
        }
    }

    private static void applyChunkLoadersDefaults(FileConfiguration config) {
        config.addDefault("features.chunk-loaders.enabled", true);
        config.addDefault("features.chunk-loaders.item.material", "BEACON");
        config.addDefault("features.chunk-loaders.item.name", "<gold>Chunk Loader</gold>");
        List<String> chunkLoaderLore = new ArrayList<>();
        chunkLoaderLore.add("<gray>Place this to keep the chunk loaded.</gray>");
        chunkLoaderLore.add("<gray>Works even when no players are online!</gray>");
        config.addDefault("features.chunk-loaders.item.lore", chunkLoaderLore);
    }
}

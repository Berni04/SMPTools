package com.smp.smptools;

import com.smp.smptools.commands.*;
import com.smp.smptools.enchants.EnchantmentManager;
import com.smp.smptools.leaderboard.LeaderboardManager;
import com.smp.smptools.skills.SkillsManager;
import com.smp.smptools.tags.TagManager;
import com.smp.smptools.tpa.TpaManager;
import com.smp.smptools.imagemap.MapManager;
import com.smp.smptools.listeners.CombatListener;
import com.smp.smptools.listeners.HomesGUIListener;
import com.smp.smptools.listeners.PrefixGUIListener;
import com.smp.smptools.listeners.LeaderboardGUIListener;
import com.smp.smptools.listeners.SkillsGUIListener;
import com.smp.smptools.listeners.*;
import com.smp.smptools.listeners.NameTagListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

public class SMPTools extends JavaPlugin {

    private static SMPTools instance;
    private File statsFile;
    private FileConfiguration statsConfig;
    private File tagsFile;
    private FileConfiguration tagsConfig;
    private File rewardsFile;
    private FileConfiguration rewardsConfig;
    private File imageMapsFile;
    private FileConfiguration imageMapsConfig;
    private LeaderboardCommand leaderboardCommand;
    private NameTagListener nameTagListener;
    private LeaderboardManager leaderboardManager;
    private TagManager tagManager;
    private TpaManager tpaManager;
    private SkillsManager skillsManager;
    private EnchantmentManager enchantmentManager;
    private MapManager mapManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("SMPTools has been enabled!");

        // Setup configs
        getConfig().addDefault("features.daily-rewards.enabled", true);
        getConfig().addDefault("features.custom-enchants.enabled", true);
        getConfig().addDefault("features.mmo-skills.enabled", true);

        // Default Daily Rewards Config
        getConfig().addDefault("features.daily-rewards.cooldown-hours", 22);
        List<String> defaultRewards = new ArrayList<>();
        defaultRewards.add("eco give %player% 100");
        defaultRewards.add("item:diamond 5");
        getConfig().addDefault("features.daily-rewards.rewards", defaultRewards);

                    // Default MMO-Skills Config
                    getConfig().addDefault("features.mmo-skills.mining.enabled", true);
                    getConfig().addDefault("features.mmo-skills.mining.double-drop-chance", "0.0015 * level"); // 7.5% at Lvl 50
                    getConfig().addDefault("features.mmo-skills.woodcutting.enabled", true);
                    getConfig().addDefault("features.mmo-skills.woodcutting.double-drop-chance", "0.0015 * level");
                    getConfig().addDefault("features.mmo-skills.excavation.enabled", true);
                    getConfig().addDefault("features.mmo-skills.excavation.double-drop-chance", "0.0015 * level");
                    // Treasure Hunter Perk
                    getConfig().addDefault("features.mmo-skills.excavation.treasure-hunter.enabled", true);
                    getConfig().addDefault("features.mmo-skills.excavation.treasure-hunter.chance", "0.0005 * level"); // 2.5% at Lvl 50
                    getConfig().addDefault("features.mmo-skills.excavation.treasure-hunter.loot.common", List.of("IRON_NUGGET 1", "GOLD_NUGGET 1"));
                    getConfig().addDefault("features.mmo-skills.excavation.treasure-hunter.loot.uncommon", List.of("GLOWSTONE_DUST 2", "QUARTZ 1"));
                                getConfig().addDefault("features.mmo-skills.excavation.treasure-hunter.loot.rare", List.of("DIAMOND 1", "NAME_TAG 1"));
                    
                                // Default Combat Skill Config
                                getConfig().addDefault("features.mmo-skills.combat.enabled", true);
                                getConfig().addDefault("features.mmo-skills.combat.critical-strike.enabled", true);
                                getConfig().addDefault("features.mmo-skills.combat.critical-strike.chance", "0.002 * level"); // 10% at Lvl 50
                                        getConfig().addDefault("features.mmo-skills.combat.critical-strike.damage-multiplier", "1.0 + (level / 10.0)"); // 1.0x + 5.0x = 6.0x at Lvl 50
                                
                                        // Sit on Stairs
                                        getConfig().addDefault("features.sit-on-stairs.enabled", true);
                                
                                        // Player Graves
                                        getConfig().addDefault("features.player-graves.enabled", true);
                                
                                        // Default Custom Enchants Config
                                        getConfig().addDefault("features.custom-enchants.telekinesis.enabled", true);        getConfig().addDefault("features.custom-enchants.telekinesis.description", "Automatically sends block drops to your inventory.");
        List<String> telekinesisApplicable = new ArrayList<>();
        telekinesisApplicable.add("PICKAXE");
        telekinesisApplicable.add("AXE");
        telekinesisApplicable.add("SHOVEL");
        telekinesisApplicable.add("HOE");
        getConfig().addDefault("features.custom-enchants.telekinesis.applicable-items", telekinesisApplicable);

        // Lumberjack Enchant
        getConfig().addDefault("features.custom-enchants.lumberjack.enabled", true);
        getConfig().addDefault("features.custom-enchants.lumberjack.description", "Breaks an entire tree at once.");
        List<String> lumberjackApplicable = new ArrayList<>();
        lumberjackApplicable.add("AXE");
        getConfig().addDefault("features.custom-enchants.lumberjack.applicable-items", lumberjackApplicable);

        // Image to Map
        getConfig().addDefault("features.image-to-map.enabled", true);

        // Music Player
        getConfig().addDefault("features.music-player.enabled", true);
        getConfig().addDefault("features.music-player.base-url", "https://raw.githubusercontent.com/YourUser/YourRepo/main/");

        getConfig().options().copyDefaults(true);
        saveConfig();

        setupStatsConfig();
        setupTagsConfig();
        setupRewardsConfig();
        setupImageMapsConfig();

        // Instantiate Managers
        this.leaderboardManager = new LeaderboardManager(this);
        this.tagManager = new TagManager(this);
        this.tpaManager = new TpaManager(this);
        if (getConfig().getBoolean("features.mmo-skills.enabled")) {
            this.skillsManager = new SkillsManager(this);
        }
        if (getConfig().getBoolean("features.custom-enchants.enabled")) {
            this.enchantmentManager = new EnchantmentManager(this);
        }
        if (getConfig().getBoolean("features.image-to-map.enabled")) {
            this.mapManager = new MapManager(this);
            this.mapManager.loadMaps();
        }

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new SleepListener(), this);
        Bukkit.getPluginManager().registerEvents(new VaultListener(this), this);
        this.nameTagListener = new NameTagListener(this);
        Bukkit.getPluginManager().registerEvents(nameTagListener, this);
        Bukkit.getPluginManager().registerEvents(new StatsListener(this), this);
        StatsCommand statsCommand = new StatsCommand(this);
        this.getCommand("stats").setExecutor(statsCommand);
        Bukkit.getPluginManager().registerEvents(new StatsGUIListener(statsCommand), this);
        Bukkit.getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new HomesGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PrefixGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LeaderboardGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TagsGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TabHealthListener(this), this);

        if (getConfig().getBoolean("features.sit-on-stairs.enabled")) {
            Bukkit.getPluginManager().registerEvents(new SitListener(this), this);
        }

        // Register Commands
        this.getCommand("fly").setExecutor(new FlyCommand());
        this.getCommand("pv").setExecutor(new PrivateVaultCommand(this));
        this.getCommand("sethome").setExecutor(new SetHomeCommand(this));
        this.getCommand("home").setExecutor(new HomeCommand(this));
        this.getCommand("delhome").setExecutor(new DelHomeCommand(this));
        this.getCommand("homes").setExecutor(new HomesCommand(this));
        this.getCommand("msg").setExecutor(new MsgCommand(this));
        this.getCommand("stats").setExecutor(new StatsCommand(this));
        this.getCommand("clearstats").setExecutor(new ClearStatsCommand(this));
        this.getCommand("prefix").setExecutor(new PrefixCommand());
        this.getCommand("color").setExecutor(new ColorCommand());
        this.getCommand("tags").setExecutor(new TagsCommand(this));
        TpaCommand tpaCommand = new TpaCommand(this);
        this.getCommand("tpr").setExecutor(tpaCommand);
        this.getCommand("tpa").setExecutor(tpaCommand);
        this.getCommand("tpd").setExecutor(tpaCommand);
        this.getCommand("tptoggle").setExecutor(tpaCommand);
        this.leaderboardCommand = new LeaderboardCommand(this);
        this.getCommand("leaderboard").setExecutor(leaderboardCommand);

        // Register conditional features
        if (getConfig().getBoolean("features.daily-rewards.enabled")) {
            this.getCommand("daily").setExecutor(new DailyRewardCommand(this));
        }

        if (getConfig().getBoolean("features.mmo-skills.enabled")) {
            Bukkit.getPluginManager().registerEvents(new SkillsListener(this), this);
            Bukkit.getPluginManager().registerEvents(new SkillsGUIListener(), this);
            Bukkit.getPluginManager().registerEvents(new CombatListener(this), this); // Register CombatListener
            this.getCommand("skills").setExecutor(new SkillsCommand(this));
        }

        if (getConfig().getBoolean("features.custom-enchants.enabled")) {
            Bukkit.getPluginManager().registerEvents(new EnchantmentListener(this), this);
            this.getCommand("cenchant").setExecutor(new CustomEnchantCommand(this));
        }

        if (getConfig().getBoolean("features.image-to-map.enabled")) {
            this.getCommand("tomap").setExecutor(new com.smp.smptools.imagemap.MapCommand(this));
        }

        if (getConfig().getBoolean("features.music-player.enabled")) {
            this.getCommand("music").setExecutor(new com.smp.smptools.music.MusicCommand(this));
        }

        // Playtime tracker
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    String uuid = player.getUniqueId().toString();
                    long newTime = getStatsConfig().getLong("stats." + uuid + ".playtime_minutes", 0) + 1;
                    getStatsConfig().set("stats." + uuid + ".playtime_minutes", newTime);
                }
                saveStatsConfig();
            }
        }.runTaskTimer(this, 0L, 1200L); // 1200 ticks = 1 minute
    }

    @Override
    public void onDisable() {
        getLogger().info("SMPTools has been disabled!");
    }

    public void setupStatsConfig() {
        statsFile = new File(getDataFolder(), "stats.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create stats.yml file!");
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }

    public FileConfiguration getStatsConfig() {
        return statsConfig;
    }

    public void saveStatsConfig() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save stats.yml file!");
        }
    }

    public void setupTagsConfig() {
        tagsFile = new File(getDataFolder(), "tags.yml");
        if (!tagsFile.exists()) {
            try {
                tagsFile.createNewFile();
                saveResource("tags.yml", true);
            } catch (IOException e) {
                getLogger().severe("Could not create tags.yml file!");
            }
        }
        tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);

        // Add default milestones if they don't exist
        if (!tagsConfig.contains("milestones")) {
            ConfigurationSection milestones = tagsConfig.createSection("milestones");

            // Blocks Broken
            createMilestone(milestones, "novice_builder", "Novice Builder", "Break 1,000 blocks", "blocks_broken", 1000);
            createMilestone(milestones, "master_builder", "Master Builder", "Break 100,000 blocks", "blocks_broken", 100000);

            // Blocks Placed
            createMilestone(milestones, "architect", "Architect", "Place 10,000 blocks", "blocks_placed", 10000);

            // Playtime
            createMilestone(milestones, "addict", "Addict", "Play for 24 hours", "playtime_minutes", 1440);
            createMilestone(milestones, "no_life", "No-Life", "Play for 7 days", "playtime_minutes", 10080);

            // Deaths
            createMilestone(milestones, "careless", "Careless", "Die 50 times", "deaths_total", 50);
            createMilestone(milestones, "cannon_fodder", "Cannon Fodder", "Die 200 times", "deaths_total", 200);

            // Player Kills
            createMilestone(milestones, "brawler", "Brawler", "Kill 10 players", "player_kills", 10);
            createMilestone(milestones, "warrior", "Warrior", "Kill 50 players", "player_kills", 50);

            // Boss Kills
            createMilestone(milestones, "dragon_slayer", "Dragon-Slayer", "Kill the Ender Dragon", "ender_dragon_kills", 1);
            createMilestone(milestones, "wither_slayer", "Wither-Slayer", "Kill the Wither", "wither_kills", 1);
            createMilestone(milestones, "deep_dweller", "Deep-Dweller", "Kill the Warden", "warden_kills", 1);

            // Dimension/Item Milestones
            createMilestone(milestones, "nether_walker", "Nether-Walker", "Enter the Nether", "enter_nether", 1);
            createMilestone(milestones, "aviator", "Aviator", "Obtain an Elytra", "obtain_elytra", 1);
            createMilestone(milestones, "ocean_master", "Ocean-Master", "Obtain a Trident", "obtain_trident", 1);
            createMilestone(milestones, "immortal", "Immortal", "Use a Totem of Undying", "use_totem", 1);

            // Ore Milestones
            createMilestone(milestones, "diamond_king", "Diamond King", "Mine 500 diamonds", "ores_mined.diamond", 500);
            createMilestone(milestones, "emerald_mogul", "Emerald Mogul", "Mine 500 emeralds", "ores_mined.emerald", 500);

            saveTagsConfig();
        }
    }

    private void createMilestone(ConfigurationSection parent, String key, String title, String description, String statistic, int value) {
        ConfigurationSection section = parent.createSection(key);
        section.set("title", title);
        section.set("description", description);
        section.set("statistic", statistic);
        section.set("value", value);
    }

    public FileConfiguration getTagsConfig() {
        return tagsConfig;
    }

    public void saveTagsConfig() {
        try {
            tagsConfig.save(tagsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save tags.yml file!");
        }
    }

    public void setupRewardsConfig() {
        rewardsFile = new File(getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            try {
                rewardsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create rewards.yml file!");
            }
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    public FileConfiguration getRewardsConfig() {
        return rewardsConfig;
    }

    public void saveRewardsConfig() {
        try {
            rewardsConfig.save(rewardsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save rewards.yml file!");
        }
    }

    public void setupImageMapsConfig() {
        imageMapsFile = new File(getDataFolder(), "imagemaps.yml");
        if (!imageMapsFile.exists()) {
            try {
                imageMapsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create imagemaps.yml file!");
            }
        }
        imageMapsConfig = YamlConfiguration.loadConfiguration(imageMapsFile);
    }

    public FileConfiguration getImageMapsConfig() {
        return imageMapsConfig;
    }

    public void saveImageMapsConfig() {
        try {
            imageMapsConfig.save(imageMapsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save imagemaps.yml file!");
        }
    }

    public static SMPTools getInstance() {
        return instance;
    }

    public NameTagListener getNameTagListener() {
        return nameTagListener;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public TpaManager getTpaManager() {
        return tpaManager;
    }

    public SkillsManager getSkillsManager() {
        return skillsManager;
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }
}


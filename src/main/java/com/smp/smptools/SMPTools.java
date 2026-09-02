package com.smp.smptools;

import com.smp.smptools.commands.LeaderboardCommand;
import com.smp.smptools.commands.StatsCommand;
import com.smp.smptools.commands.TpaCommand;
import com.smp.smptools.commands.SecretSantaCommand;
import com.smp.smptools.config.CommandRegistry;
import com.smp.smptools.config.ConfigDefaults;
import com.smp.smptools.config.ListenerRegistry;
import com.smp.smptools.config.MessageManager;
import com.smp.smptools.utils.ConfigValidator;
import com.smp.smptools.enchants.EnchantmentManager;
import com.smp.smptools.leaderboard.LeaderboardManager;
import com.smp.smptools.skills.SkillsManager;
import com.smp.smptools.tags.TagManager;
import com.smp.smptools.tpa.TpaManager;
import com.smp.smptools.teleport.TeleportManager;
import com.smp.smptools.teleport.TeleportListener;
import com.smp.smptools.sleep.SleepManager;
import com.smp.smptools.listeners.SleepListener;
import com.smp.smptools.chat.ChatManager;
import com.smp.smptools.chunkloaders.ChunkLoaderManager;
import com.smp.smptools.missions.MissionManager;
import com.smp.smptools.accelerators.CropAccelerator;
import com.smp.smptools.christmas.AdventManager;
import com.smp.smptools.christmas.SecretSantaManager;
import com.smp.smptools.managers.ChristmasWorldManager;
import com.smp.smptools.managers.PortalManager;
import com.smp.smptools.imagemap.MapManager;
import com.smp.smptools.listeners.CombatListener;
import com.smp.smptools.listeners.HomesGUIListener;
import com.smp.smptools.listeners.PrefixGUIListener;
import com.smp.smptools.listeners.LeaderboardGUIListener;
import com.smp.smptools.listeners.SkillsGUIListener;
import com.smp.smptools.listeners.AdventGUIListener;
import com.smp.smptools.listeners.EnchantmentListener;
import com.smp.smptools.listeners.MissionGUIListener;
import com.smp.smptools.listeners.MissionNPCListener;
import com.smp.smptools.listeners.NPCListener;
import com.smp.smptools.listeners.NameTagListener;
import com.smp.smptools.listeners.PortalListener;
import com.smp.smptools.listeners.ResourcePackListener;
import com.smp.smptools.listeners.SitListener;
import com.smp.smptools.listeners.SkillsListener;
import com.smp.smptools.managers.NPCManager;
import com.smp.smptools.storage.StorageManager;
import com.smp.smptools.managers.DialogueManager;
import com.smp.smptools.managers.BlackFridayManager;
import com.smp.smptools.events.EventManager;
import com.smp.smptools.events.gui.EventGUI;
import com.smp.smptools.events.seasonal.SeasonalManager;
import com.smp.smptools.events.seasonal.SeasonalListener;
import com.smp.smptools.events.seasonal.gui.SeasonalGUI;
import com.smp.smptools.events.seasonal.gui.HalloweenGUI;
import com.smp.smptools.events.seasonal.gui.EasterGUI;
import com.smp.smptools.events.seasonal.commands.SeasonalCommand;
import com.smp.smptools.events.seasonal.commands.HalloweenCommand;
import com.smp.smptools.events.seasonal.commands.EasterCommand;
import com.smp.smptools.events.seasonal.commands.SummerCommand;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.smp.smptools.utils.AsyncConfigHelper;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private TeleportManager teleportManager;
    private SleepManager sleepManager;
    private ChatManager chatManager;
    private ChunkLoaderManager chunkLoaderManager;
    private CropAccelerator cropAccelerator; // Declare CropAccelerator
    private MissionManager missionManager;
    private AdventManager adventManager;
    private ChristmasWorldManager christmasWorldManager;
    private PortalManager portalManager;
    private NPCManager npcManager;
    private DialogueManager dialogueManager;
    private BlackFridayManager blackFridayManager;
    private com.smp.smptools.listeners.BlackFridayListener blackFridayListener;
    private MessageManager messageManager;
    private StorageManager storageManager;
    private com.smp.smptools.afk.AFKManager afkManager;
    private com.smp.smptools.trade.TradeManager tradeManager;
    private com.smp.smptools.trails.TrailManager trailManager;
    private com.smp.smptools.bounty.BountyManager bountyManager;
    private com.smp.smptools.locks.LockManager lockManager;
    private com.smp.smptools.listeners.InvseeGUIListener invseeGUIListener;
    private com.smp.smptools.listeners.BountyGUIListener bountyGUIListener;
    private File eventsFile;
    private FileConfiguration eventsConfig;
    private EventManager eventManager;
    private EventGUI eventGUI;
    private com.smp.smptools.artifacts.ArtifactManager artifactManager;
    private com.smp.smptools.artifacts.gui.ArtifactEquipmentGUI artifactEquipmentGUI;
    private File seasonalFile;
    private FileConfiguration seasonalConfig;
    private SeasonalManager seasonalManager;
    private SeasonalGUI seasonalGUI;
    private HalloweenGUI halloweenGUI;
    private EasterGUI easterGUI;
    private com.smp.smptools.christmas.KrampusManager krampusManager;


    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("SMPTools has been enabled!");

        // Setup configs
        ConfigDefaults.applyDefaults(this);
        ConfigValidator.validate(this);

        setupStatsConfig();
        setupTagsConfig();
        setupRewardsConfig();
        setupImageMapsConfig();
        setupEventsConfig();
        setupSeasonalConfig();

        // Initialize Storage Manager
        this.storageManager = new StorageManager(this);

        // Instantiate Managers conditionally based on feature flags in config.yml
        if (getConfig().getBoolean("features.afk.enabled", true)) {
            this.afkManager = new com.smp.smptools.afk.AFKManager(this);
        }
        if (getConfig().getBoolean("features.remote-trade.enabled", true)) {
            this.tradeManager = new com.smp.smptools.trade.TradeManager(this);
        }
        if (getConfig().getBoolean("features.trails.enabled", true)) {
            this.trailManager = new com.smp.smptools.trails.TrailManager(this);
        }
        if (getConfig().getBoolean("features.bounties.enabled", true)) {
            this.bountyManager = new com.smp.smptools.bounty.BountyManager(this);
        }
        if (getConfig().getBoolean("features.container-locks.enabled", true)) {
            this.lockManager = new com.smp.smptools.locks.LockManager(this);
        }
        if (getConfig().getBoolean("features.leaderboard.enabled", true)) {
            this.leaderboardManager = new LeaderboardManager(this);
        }
        if (getConfig().getBoolean("features.tags.enabled", true)) {
            this.tagManager = new TagManager(this);
        }
        if (getConfig().getBoolean("features.tpa.enabled", true)) {
            this.tpaManager = new TpaManager(this);
        }
        this.teleportManager = new TeleportManager(this);
        if (getConfig().getBoolean("features.sleep-voting.enabled", true)) {
            this.sleepManager = new SleepManager(this);
        }
        this.chatManager = new ChatManager(this);
        if (getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            this.chunkLoaderManager = new ChunkLoaderManager(this);
        }
        if (getConfig().getBoolean("features.mmo-skills.enabled", true)) {
            this.skillsManager = new SkillsManager(this);
        }
        if (getConfig().getBoolean("features.custom-enchants.enabled", true)) {
            this.enchantmentManager = new EnchantmentManager(this);
        }
        if (getConfig().getBoolean("features.image-to-map.enabled", true)) {
            this.mapManager = new MapManager(this);
            this.mapManager.loadMaps();
        }
        if (getConfig().getBoolean("features.missions.enabled", true)) {
            this.missionManager = new MissionManager(this);
        }
        if (getConfig().getBoolean("features.christmas.enabled", true)) {
            this.adventManager = new AdventManager(this);
            this.christmasWorldManager = new ChristmasWorldManager(this);
            this.portalManager = new PortalManager(this);
        }
        if (getConfig().getBoolean("features.npcs.enabled", true)) {
            this.npcManager = new NPCManager(this);
        }
        this.dialogueManager = new DialogueManager(this);
        if (getConfig().getBoolean("features.blackfriday.enabled", true)) {
            this.blackFridayManager = new BlackFridayManager(this);
        }
        this.messageManager = new MessageManager(this);

        // Register Listeners and Commands
        this.nameTagListener = new NameTagListener(this);
        Bukkit.getPluginManager().registerEvents(nameTagListener, this);

        StatsCommand statsCommand = new StatsCommand(this);

        AdventGUIListener adventGUIListener = getConfig().getBoolean("features.christmas.enabled", true)
                ? new AdventGUIListener(this, adventManager) : null;

        ListenerRegistry.registerCoreListeners(this, statsCommand, adventGUIListener);

        // Register Accelerators
        if (getConfig().getBoolean("features.accelerated-growth.enabled", true)) {
            this.cropAccelerator = new CropAccelerator(this,
                    getConfig().getDouble("features.accelerated-growth.multiplier", 2.0));
            this.cropAccelerator.runTaskTimer(this, 0L, 20L); // Run every second
        }

        // Conditional listeners
        if (getConfig().getBoolean("features.sleep-voting.enabled")) {
            Bukkit.getPluginManager().registerEvents(new SleepListener(this), this);
        }
        if (getConfig().getBoolean("features.sit-on-stairs.enabled")) {
            Bukkit.getPluginManager().registerEvents(new SitListener(this), this);
        }
        if (getConfig().getBoolean("features.mmo-skills.enabled")) {
            Bukkit.getPluginManager().registerEvents(new SkillsListener(this), this);
            Bukkit.getPluginManager().registerEvents(new SkillsGUIListener(), this);
            Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        }
        if (getConfig().getBoolean("features.custom-enchants.enabled")) {
            Bukkit.getPluginManager().registerEvents(new EnchantmentListener(this), this);
        }
        if (getConfig().getBoolean("features.meme-sounds.enabled")) {
            Bukkit.getPluginManager().registerEvents(new ResourcePackListener(this), this);
        }
        if (getConfig().getBoolean("features.afk.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.listeners.AFKListener(this), this);
        }
        if (getConfig().getBoolean("features.remote-trade.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.listeners.TradeListener(this), this);
        }

        // Register Commands
        TpaCommand tpaCommand = new TpaCommand(this);
        this.leaderboardCommand = new LeaderboardCommand(this);
        CommandRegistry.registerAll(this, statsCommand, leaderboardCommand, tpaCommand, adventGUIListener);
        CommandRegistry.registerConditionalCommands(this);

        // Load NPCs
        if (npcManager != null && getConfig().getBoolean("features.npcs.enabled", true)) {
            npcManager.loadNPCs();
        }

        // Christmas features (conditional)
        if (getConfig().getBoolean("features.christmas.enabled", true)) {
            // Secret Santa
            SecretSantaManager secretSantaManager = new SecretSantaManager(this);
            Objects.requireNonNull(getCommand("secretsanta")).setExecutor(new SecretSantaCommand(this, secretSantaManager));

            // Present Hunt
            com.smp.smptools.christmas.PresentManager presentManager = new com.smp.smptools.christmas.PresentManager(this);
            this.getCommand("present").setExecutor(new com.smp.smptools.commands.PresentCommand(this, presentManager));
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.listeners.PresentListener(presentManager), this);

            // Festive Mobs
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.listeners.FestiveMobsListener(this), this);

            // Snowball Warfare
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.listeners.SnowballListener(this), this);

            // Krampus Night
            this.krampusManager = new com.smp.smptools.christmas.KrampusManager(this);
            this.getCommand("krampus").setExecutor(new com.smp.smptools.commands.KrampusCommand(this, krampusManager));
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.listeners.KrampusListener(this, krampusManager),
                    this);
        }

        // Black Friday (conditional)
        if (getConfig().getBoolean("features.blackfriday.enabled", true)) {
            this.blackFridayListener = new com.smp.smptools.listeners.BlackFridayListener(this, blackFridayManager);
            Bukkit.getPluginManager().registerEvents(this.blackFridayListener, this);
            Objects.requireNonNull(getCommand("blackfriday"))
                    .setExecutor(new com.smp.smptools.commands.BlackFridayCommand(blackFridayManager));
        }

        // Mini-Events Subsystem
        if (getConfig().getBoolean("features.events.enabled", true)) {
            this.eventManager = new EventManager(this);
            this.eventManager.initialize();
            this.eventGUI = new EventGUI(this, eventManager);
            if (getCommand("event") != null) {
                getCommand("event").setExecutor(new com.smp.smptools.events.commands.EventCommand(this, eventManager, eventGUI));
            }
        }

        // Custom Artifacts Subsystem
        if (getConfig().getBoolean("features.artifacts.enabled", true)) {
            this.artifactManager = new com.smp.smptools.artifacts.ArtifactManager(this);
            this.artifactEquipmentGUI = new com.smp.smptools.artifacts.gui.ArtifactEquipmentGUI(this, artifactManager);
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.artifacts.ArtifactListener(this, artifactManager), this);
            if (getCommand("artifacts") != null) {
                getCommand("artifacts").setExecutor(new com.smp.smptools.artifacts.commands.ArtifactCommand(this, artifactManager, artifactEquipmentGUI));
            }
        }

        // Seasonal Events Subsystem
        if (getConfig().getBoolean("features.seasonal.enabled", true)) {
            this.seasonalManager = new SeasonalManager(this);
            this.seasonalGUI = new SeasonalGUI(this, seasonalManager);
            this.halloweenGUI = new HalloweenGUI(this, seasonalManager);
            this.easterGUI = new EasterGUI(this, seasonalManager);

            Bukkit.getPluginManager().registerEvents(new SeasonalListener(this, seasonalManager), this);

            if (getCommand("seasonal") != null) {
                getCommand("seasonal").setExecutor(new SeasonalCommand(this, seasonalManager, seasonalGUI));
            }
            if (getCommand("halloween") != null) {
                getCommand("halloween").setExecutor(new HalloweenCommand(this, seasonalManager, halloweenGUI));
            }
            if (getCommand("easter") != null) {
                getCommand("easter").setExecutor(new EasterCommand(this, seasonalManager, easterGUI));
            }
            if (getCommand("summer") != null) {
                getCommand("summer").setExecutor(new SummerCommand(this, seasonalManager));
            }
        }

        startStatsSaverTask();
    }

    private void startStatsSaverTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                long totalTicks = player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                long totalMinutes = totalTicks / (20 * 60);
                getStatsConfig().set("stats." + player.getUniqueId() + ".playtime_minutes", totalMinutes);
                if (storageManager != null && storageManager.getProvider() != null) {
                    storageManager.getProvider().saveStat(player.getUniqueId(), "playtime_minutes", totalMinutes);
                }
            }
            saveStatsConfig();
        }, 6000L, 6000L); // Run every 5 minutes (6000 ticks)
    }

    @Override
    public void onDisable() {
        getLogger().info("SMPTools has been disabled!");
        
        // Cancel all scheduled tasks to prevent firing during/after disable
        Bukkit.getScheduler().cancelTasks(this);
        
        if (tradeManager != null) {
            tradeManager.cleanup();
        }
        if (chunkLoaderManager != null) {
            chunkLoaderManager.unloadAllChunks(); // Unload all force-loaded chunks
        }
        if (missionManager != null) {
            missionManager.savePlayerData();
        }
        if (npcManager != null) {
            npcManager.removeAllNPCs();
        }
        if (dialogueManager != null) {
            dialogueManager.cleanupAll();
        }
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (artifactManager != null) {
            artifactManager.savePouchData();
        }
        if (krampusManager != null) {
            krampusManager.cleanupAll();
        }
        if (blackFridayListener != null) {
            blackFridayListener.restoreAllActive();
        }
        if (seasonalManager != null) {
            seasonalManager.saveLocations();
            seasonalManager.savePlayerData();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (bountyManager != null) {
            bountyManager.shutdown();
        }
        // Save configs
        saveStatsConfig();
        saveTagsConfig();
        saveRewardsConfig();
        saveImageMapsConfig();
        AsyncConfigHelper.shutdown();
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
        AsyncConfigHelper.saveConfigAsync(this, statsConfig, statsFile, "stats.yml");
    }

    public void setupTagsConfig() {
        tagsFile = new File(getDataFolder(), "tags.yml");
        if (!tagsFile.exists()) {
            try {
                tagsFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create tags.yml file!");
            }
        }
        tagsConfig = YamlConfiguration.loadConfiguration(tagsFile);

        // Add default milestones if they don't exist
        if (!tagsConfig.contains("milestones")) {
            ConfigurationSection milestones = tagsConfig.createSection("milestones");

            // Blocks Broken
            createMilestone(milestones, "novice_builder", "Novice Builder", "Break 1,000 blocks", "blocks_broken",
                    1000);
            createMilestone(milestones, "master_builder", "Master Builder", "Break 100,000 blocks", "blocks_broken",
                    100000);

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
            createMilestone(milestones, "dragon_slayer", "Dragon-Slayer", "Kill the Ender Dragon", "ender_dragon_kills",
                    1);
            createMilestone(milestones, "wither_slayer", "Wither-Slayer", "Kill the Wither", "wither_kills", 1);
            createMilestone(milestones, "deep_dweller", "Deep-Dweller", "Kill the Warden", "warden_kills", 1);

            // Dimension/Item Milestones
            createMilestone(milestones, "nether_walker", "Nether-Walker", "Enter the Nether", "enter_nether", 1);
            createMilestone(milestones, "aviator", "Aviator", "Obtain an Elytra", "obtain_elytra", 1);
            createMilestone(milestones, "ocean_master", "Ocean-Master", "Obtain a Trident", "obtain_trident", 1);
            createMilestone(milestones, "immortal", "Immortal", "Use a Totem of Undying", "use_totem", 1);

            // Ore Milestones
            createMilestone(milestones, "diamond_king", "Diamond King", "Mine 500 diamonds", "ores_mined.diamond", 500);
            createMilestone(milestones, "emerald_mogul", "Emerald Mogul", "Mine 500 emeralds", "ores_mined.emerald",
                    500);

            // Mob Kills
            createMilestone(milestones, "zombie_hunter", "Zombie Hunter", "Kill 1,000 zombies", "mob_kills.zombie",
                    1000);
            createMilestone(milestones, "skeleton_archer", "Skeleton Archer", "Kill 1,000 skeletons",
                    "mob_kills.skeleton", 1000);
            createMilestone(milestones, "creeper_destroyer", "Creeper Destroyer", "Kill 500 creepers",
                    "mob_kills.creeper", 500);

            // Crafting
            createMilestone(milestones, "crafter", "Crafter", "Craft 1,000 items", "items_crafted", 1000);
            createMilestone(milestones, "master_crafter", "Master Crafter", "Craft 10,000 items", "items_crafted",
                    10000);

            // Movement
            createMilestone(milestones, "hiker", "Hiker", "Walk 100,000 blocks", "distance_walked_cm", 10000000); // 100km
            createMilestone(milestones, "sprinter", "Sprinter", "Sprint 100,000 blocks", "distance_sprinted_cm",
                    10000000); // 100km
            createMilestone(milestones, "swimmer", "Swimmer", "Swim 10,000 blocks", "distance_swam_cm", 1000000); // 10km

            saveTagsConfig();
        }
    }

    private void createMilestone(ConfigurationSection parent, String key, String title, String description,
            String statistic, int value) {
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
        AsyncConfigHelper.saveConfigAsync(this, tagsConfig, tagsFile, "tags.yml");
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
        AsyncConfigHelper.saveConfigAsync(this, rewardsConfig, rewardsFile, "rewards.yml");
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
        AsyncConfigHelper.saveConfigAsync(this, imageMapsConfig, imageMapsFile, "imagemaps.yml");
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

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public SleepManager getSleepManager() {
        return sleepManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public ChunkLoaderManager getChunkLoaderManager() {
        return chunkLoaderManager;
    }

    public MissionManager getMissionManager() {
        return missionManager;
    }

    public AdventManager getAdventManager() {
        return adventManager;
    }

    public ChristmasWorldManager getChristmasWorldManager() {
        return christmasWorldManager;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public NPCManager getNPCManager() {
        return npcManager;
    }

    public DialogueManager getDialogueManager() {
        return dialogueManager;
    }

    public BlackFridayManager getBlackFridayManager() {
        return blackFridayManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    public com.smp.smptools.afk.AFKManager getAFKManager() {
        return afkManager;
    }

    public com.smp.smptools.trade.TradeManager getTradeManager() {
        return tradeManager;
    }

    public com.smp.smptools.trails.TrailManager getTrailManager() {
        return trailManager;
    }

    public com.smp.smptools.bounty.BountyManager getBountyManager() {
        return bountyManager;
    }

    public com.smp.smptools.locks.LockManager getLockManager() {
        return lockManager;
    }

    public com.smp.smptools.listeners.InvseeGUIListener getInvseeGUIListener() {
        return invseeGUIListener;
    }

    public void setInvseeGUIListener(com.smp.smptools.listeners.InvseeGUIListener listener) {
        this.invseeGUIListener = listener;
    }

    public com.smp.smptools.listeners.BountyGUIListener getBountyGUIListener() {
        return bountyGUIListener;
    }

    public void setBountyGUIListener(com.smp.smptools.listeners.BountyGUIListener listener) {
        this.bountyGUIListener = listener;
    }

    private void setupEventsConfig() {
        eventsFile = new File(getDataFolder(), "events.yml");
        if (!eventsFile.exists()) {
            eventsFile.getParentFile().mkdirs();
            saveResource("events.yml", false);
        }
        eventsConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(eventsFile);
    }

    public FileConfiguration getEventsConfig() {
        if (eventsConfig == null) {
            setupEventsConfig();
        }
        return eventsConfig;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public com.smp.smptools.artifacts.ArtifactManager getArtifactManager() {
        return artifactManager;
    }

    private void setupSeasonalConfig() {
        seasonalFile = new File(getDataFolder(), "seasonal.yml");
        if (!seasonalFile.exists()) {
            seasonalFile.getParentFile().mkdirs();
            saveResource("seasonal.yml", false);
        }
        seasonalConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(seasonalFile);
    }

    public FileConfiguration getSeasonalConfig() {
        if (seasonalConfig == null) {
            setupSeasonalConfig();
        }
        return seasonalConfig;
    }

    public SeasonalManager getSeasonalManager() {
        return seasonalManager;
    }
}

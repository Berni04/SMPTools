package com.smp.smptools;

import com.smp.smptools.commands.*;
import com.smp.smptools.leaderboard.LeaderboardManager;
import com.smp.smptools.listeners.HomesGUIListener;
import com.smp.smptools.listeners.PrefixGUIListener;
import com.smp.smptools.listeners.LeaderboardGUIListener;
import com.smp.smptools.listeners.*;
import com.smp.smptools.listeners.NameTagListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class SMPTools extends JavaPlugin {

    private static SMPTools instance;
    private File statsFile;
    private FileConfiguration statsConfig;
    private LeaderboardCommand leaderboardCommand;
    private NameTagListener nameTagListener;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        instance = this;
        this.leaderboardManager = new LeaderboardManager(this);
        getLogger().info("SMPTools has been enabled!");

        // Setup configs
        saveDefaultConfig();
        setupStatsConfig();

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
        Bukkit.getPluginManager().registerEvents(new TabHealthListener(this), this);

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
        this.leaderboardCommand = new LeaderboardCommand(this);
        this.getCommand("leaderboard").setExecutor(leaderboardCommand);

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

    public static SMPTools getInstance() {
        return instance;
    }

    public NameTagListener getNameTagListener() {
        return nameTagListener;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }
}


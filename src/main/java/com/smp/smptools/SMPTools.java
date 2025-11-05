package com.smp.smptools;

import com.smp.smptools.commands.*;
import com.smp.smptools.listeners.NameTagListener;
import com.smp.smptools.listeners.SleepListener;
import com.smp.smptools.listeners.StatsListener;
import com.smp.smptools.listeners.StatsGUIListener;
import com.smp.smptools.listeners.VaultListener;
import com.smp.smptools.listeners.JoinLeaveListener;
import com.smp.smptools.listeners.PrefixGUIListener;
import com.smp.smptools.listeners.ColorGUIListener;
import com.smp.smptools.listeners.ChatListener;
import com.smp.smptools.listeners.TabHealthListener;
import com.smp.smptools.listeners.LeaderboardGUIListener;
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

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("SMPTools has been enabled!");

        // Setup configs
        saveDefaultConfig();
        setupStatsConfig();

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new SleepListener(), this);
        Bukkit.getPluginManager().registerEvents(new VaultListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NameTagListener(this), this);
        Bukkit.getPluginManager().registerEvents(new StatsListener(this), this);
        StatsCommand statsCommand = new StatsCommand(this);
        this.getCommand("stats").setExecutor(statsCommand);
        Bukkit.getPluginManager().registerEvents(new StatsGUIListener(statsCommand), this);
        Bukkit.getPluginManager().registerEvents(new JoinLeaveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PrefixGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ColorGUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
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
        Bukkit.getPluginManager().registerEvents(new LeaderboardGUIListener(leaderboardCommand), this);
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
}


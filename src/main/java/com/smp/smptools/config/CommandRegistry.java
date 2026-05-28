package com.smp.smptools.config;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.*;
import com.smp.smptools.managers.AdventGUIListener;
import org.bukkit.Bukkit;

import java.util.Objects;

public final class CommandRegistry {

    private CommandRegistry() {
        // Prevent instantiation
    }

    public static void registerAll(SMPTools plugin, LeaderboardCommand leaderboardCommand,
                                   TpaCommand tpaCommand, AdventGUIListener adventGUIListener) {
        // Core commands
        plugin.getCommand("fly").setExecutor(new FlyCommand());
        plugin.getCommand("pv").setExecutor(new PrivateVaultCommand(plugin));
        plugin.getCommand("sethome").setExecutor(new SetHomeCommand(plugin));
        plugin.getCommand("home").setExecutor(new HomeCommand(plugin));
        plugin.getCommand("delhome").setExecutor(new DelHomeCommand(plugin));
        plugin.getCommand("homes").setExecutor(new HomesCommand(plugin));
        plugin.getCommand("msg").setExecutor(new MsgCommand(plugin));
        plugin.getCommand("clearstats").setExecutor(new ClearStatsCommand(plugin));
        plugin.getCommand("prefix").setExecutor(new PrefixCommand());
        plugin.getCommand("color").setExecutor(new ColorCommand());
        plugin.getCommand("tags").setExecutor(new TagsCommand(plugin));

        // TPA commands
        plugin.getCommand("tpr").setExecutor(tpaCommand);
        plugin.getCommand("tpa").setExecutor(tpaCommand);
        plugin.getCommand("tpd").setExecutor(tpaCommand);
        plugin.getCommand("tptoggle").setExecutor(tpaCommand);

        // Leaderboard
        plugin.getCommand("leaderboard").setExecutor(leaderboardCommand);

        // Feature commands
        Objects.requireNonNull(plugin.getCommand("givechunkloader")).setExecutor(new ChunkLoaderCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("invsee")).setExecutor(new InvseeCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("troll")).setExecutor(new TrollCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("missions")).setExecutor(new MissionCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("sudo")).setExecutor(new SudoCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("customitem")).setExecutor(new CustomItemCommand());
        Objects.requireNonNull(plugin.getCommand("r")).setExecutor(new ReplyCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("rename")).setExecutor(new RenameCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("advent")).setExecutor(new AdventCommand(adventGUIListener));
        Objects.requireNonNull(plugin.getCommand("npc")).setExecutor(new NPCCommand(plugin));

        // Utility commands
        plugin.getCommand("uptime").setExecutor(new UptimeCommand());
        plugin.getCommand("ping").setExecutor(new PingCommand());
    }

    public static void registerConditionalCommands(SMPTools plugin) {
        // Daily rewards
        if (plugin.getConfig().getBoolean("features.daily-rewards.enabled")) {
            plugin.getCommand("daily").setExecutor(new DailyRewardCommand(plugin));
        }

        // MMO Skills
        if (plugin.getConfig().getBoolean("features.mmo-skills.enabled")) {
            plugin.getCommand("skills").setExecutor(new SkillsCommand(plugin));
        }

        // Custom enchants
        if (plugin.getConfig().getBoolean("features.custom-enchants.enabled")) {
            plugin.getCommand("cenchant").setExecutor(new CustomEnchantCommand(plugin));
        }

        // Image to map
        if (plugin.getConfig().getBoolean("features.image-to-map.enabled")) {
            plugin.getCommand("tomap").setExecutor(new com.smp.smptools.imagemap.MapCommand(plugin));
        }

        // Music player
        if (plugin.getConfig().getBoolean("features.music-player.enabled")) {
            plugin.getCommand("music").setExecutor(new com.smp.smptools.music.MusicCommand(plugin));
        }

        // Ride command
        if (plugin.getConfig().getBoolean("features.ride.enabled")) {
            plugin.getCommand("ride").setExecutor(new RideCommand());
        }

        // Meme sounds
        if (plugin.getConfig().getBoolean("features.meme-sounds.enabled")) {
            plugin.getCommand("sound").setExecutor(new SoundCommand(plugin));
        }

        // Sleep voting
        if (plugin.getConfig().getBoolean("features.sleep-voting.enabled")) {
            plugin.getCommand("sleepvote").setExecutor(new com.smp.smptools.sleep.SleepVoteCommand(plugin));
        }
    }
}

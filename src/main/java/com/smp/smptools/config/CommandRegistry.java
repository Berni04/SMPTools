package com.smp.smptools.config;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.AdventCommand;
import com.smp.smptools.commands.ChunkLoaderCommand;
import com.smp.smptools.commands.ClearStatsCommand;
import com.smp.smptools.commands.ColorCommand;
import com.smp.smptools.commands.CustomEnchantCommand;
import com.smp.smptools.commands.CustomItemCommand;
import com.smp.smptools.commands.DailyRewardCommand;
import com.smp.smptools.commands.DelHomeCommand;
import com.smp.smptools.commands.FlyCommand;
import com.smp.smptools.commands.HomeCommand;
import com.smp.smptools.commands.HomesCommand;
import com.smp.smptools.commands.InvseeCommand;
import com.smp.smptools.commands.LeaderboardCommand;
import com.smp.smptools.commands.MissionCommand;
import com.smp.smptools.commands.MsgCommand;
import com.smp.smptools.commands.NPCCommand;
import com.smp.smptools.commands.PingCommand;
import com.smp.smptools.commands.PrefixCommand;
import com.smp.smptools.commands.PrivateVaultCommand;
import com.smp.smptools.commands.ReplyCommand;
import com.smp.smptools.commands.RenameCommand;
import com.smp.smptools.commands.RideCommand;
import com.smp.smptools.commands.SetHomeCommand;
import com.smp.smptools.commands.SkillsCommand;
import com.smp.smptools.commands.SoundCommand;
import com.smp.smptools.commands.SudoCommand;
import com.smp.smptools.commands.TagsCommand;
import com.smp.smptools.commands.TpaCommand;
import com.smp.smptools.commands.TrollCommand;
import com.smp.smptools.commands.UptimeCommand;
import com.smp.smptools.listeners.AdventGUIListener;
import org.bukkit.Bukkit;

import java.util.Objects;

public final class CommandRegistry {

    private CommandRegistry() {
        // Prevent instantiation
    }

    public static void registerAll(SMPTools plugin, LeaderboardCommand leaderboardCommand,
                                   TpaCommand tpaCommand, AdventGUIListener adventGUIListener) {
        // Fly
        if (plugin.getConfig().getBoolean("features.fly.enabled", true)) {
            plugin.getCommand("fly").setExecutor(new FlyCommand(plugin));
        }

        // Private Vault
        if (plugin.getConfig().getBoolean("features.private-vault.enabled", true)) {
            plugin.getCommand("pv").setExecutor(new PrivateVaultCommand(plugin));
        }

        // Homes
        if (plugin.getConfig().getBoolean("features.homes.enabled", true)) {
            plugin.getCommand("sethome").setExecutor(new SetHomeCommand(plugin));
            plugin.getCommand("home").setExecutor(new HomeCommand(plugin));
            plugin.getCommand("delhome").setExecutor(new DelHomeCommand(plugin));
            plugin.getCommand("homes").setExecutor(new HomesCommand(plugin));
        }

        // Messaging
        if (plugin.getConfig().getBoolean("features.msg.enabled", true)) {
            plugin.getCommand("msg").setExecutor(new MsgCommand(plugin));
            Objects.requireNonNull(plugin.getCommand("r")).setExecutor(new ReplyCommand(plugin));
        }

        // Stats & Leaderboard
        if (plugin.getConfig().getBoolean("features.stats.enabled", true)) {
            plugin.getCommand("clearstats").setExecutor(new ClearStatsCommand(plugin));
        }
        if (plugin.getConfig().getBoolean("features.leaderboard.enabled", true)) {
            plugin.getCommand("leaderboard").setExecutor(leaderboardCommand);
        }

        // Prefix & Color
        if (plugin.getConfig().getBoolean("features.prefix-color.enabled", true)) {
            plugin.getCommand("prefix").setExecutor(new PrefixCommand(plugin));
            plugin.getCommand("color").setExecutor(new ColorCommand(plugin));
        }

        // Tags
        if (plugin.getConfig().getBoolean("features.tags.enabled", true)) {
            plugin.getCommand("tags").setExecutor(new TagsCommand(plugin));
        }

        // TPA commands
        if (plugin.getConfig().getBoolean("features.tpa.enabled", true)) {
            plugin.getCommand("tpr").setExecutor(tpaCommand);
            plugin.getCommand("tpa").setExecutor(tpaCommand);
            plugin.getCommand("tpd").setExecutor(tpaCommand);
            plugin.getCommand("tptoggle").setExecutor(tpaCommand);
        }

        // Chunk Loaders
        if (plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            Objects.requireNonNull(plugin.getCommand("givechunkloader")).setExecutor(new ChunkLoaderCommand(plugin));
        }

        // Invsee
        if (plugin.getConfig().getBoolean("features.invsee.enabled", true)) {
            Objects.requireNonNull(plugin.getCommand("invsee")).setExecutor(new InvseeCommand(plugin));
        }

        // Troll
        if (plugin.getConfig().getBoolean("features.troll.enabled", true)) {
            Objects.requireNonNull(plugin.getCommand("troll")).setExecutor(new TrollCommand(plugin));
        }

        // Missions
        if (plugin.getConfig().getBoolean("features.missions.enabled", true)) {
            Objects.requireNonNull(plugin.getCommand("missions")).setExecutor(new MissionCommand(plugin));
        }

        // Sudo
        if (plugin.getConfig().getBoolean("features.sudo.enabled", true)) {
            Objects.requireNonNull(plugin.getCommand("sudo")).setExecutor(new SudoCommand(plugin));
        }

        // Rename
        if (plugin.getConfig().getBoolean("features.item-rename.enabled", true)) {
            Objects.requireNonNull(plugin.getCommand("rename")).setExecutor(new RenameCommand(plugin));
        }

        // NPCs
        if (plugin.getConfig().getBoolean("features.npcs.enabled", true)) {
            Objects.requireNonNull(plugin.getCommand("npc")).setExecutor(new NPCCommand(plugin));
        }

        Objects.requireNonNull(plugin.getCommand("customitem")).setExecutor(new CustomItemCommand(plugin));
        Objects.requireNonNull(plugin.getCommand("advent")).setExecutor(new AdventCommand(plugin, adventGUIListener));

        // Utility commands
        plugin.getCommand("uptime").setExecutor(new UptimeCommand(plugin));
        plugin.getCommand("ping").setExecutor(new PingCommand(plugin));
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
            plugin.getCommand("ride").setExecutor(new RideCommand(plugin));
        }

        // Meme sounds
        if (plugin.getConfig().getBoolean("features.meme-sounds.enabled")) {
            plugin.getCommand("sound").setExecutor(new SoundCommand(plugin));
        }

        // Sleep voting
        if (plugin.getConfig().getBoolean("features.sleep-voting.enabled")) {
            plugin.getCommand("sleepvote").setExecutor(new com.smp.smptools.sleep.SleepVoteCommand(plugin));
        }

        // AFK Command
        if (plugin.getConfig().getBoolean("features.afk.enabled", true)) {
            plugin.getCommand("afk").setExecutor(new com.smp.smptools.commands.AFKCommand(plugin));
        }

        // Remote Trade
        if (plugin.getConfig().getBoolean("features.remote-trade.enabled", true)) {
            plugin.getCommand("trade").setExecutor(new com.smp.smptools.commands.TradeCommand(plugin));
        }

        // Particle Trails
        if (plugin.getConfig().getBoolean("features.trails.enabled", true)) {
            plugin.getCommand("trails").setExecutor(new com.smp.smptools.commands.TrailsCommand(plugin));
        }

        // Bounties
        if (plugin.getConfig().getBoolean("features.bounties.enabled", true)) {
            plugin.getCommand("bounty").setExecutor(new com.smp.smptools.commands.BountyCommand(plugin, plugin.getBountyGUIListener()));
        }

        // Container Locks
        if (plugin.getConfig().getBoolean("features.container-locks.enabled", true)) {
            var lockCmd = new com.smp.smptools.commands.LockCommand(plugin);
            plugin.getCommand("lock").setExecutor(lockCmd);
            plugin.getCommand("unlock").setExecutor(lockCmd);
            plugin.getCommand("trust").setExecutor(lockCmd);
            plugin.getCommand("untrust").setExecutor(lockCmd);
        }
    }
}

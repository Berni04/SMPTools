package com.smp.smptools.config;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.StatsCommand;
import com.smp.smptools.listeners.AdvancementListener;
import com.smp.smptools.listeners.ChatListener;
import com.smp.smptools.listeners.ChunkLoaderListener;
import com.smp.smptools.listeners.ChristmasWorldListener;
import com.smp.smptools.listeners.ElytraTrailListener;
import com.smp.smptools.listeners.HomesGUIListener;
import com.smp.smptools.listeners.InvseeGUIListener;
import com.smp.smptools.listeners.JoinLeaveListener;
import com.smp.smptools.listeners.LeaderboardGUIListener;
import com.smp.smptools.listeners.MissionGUIListener;
import com.smp.smptools.listeners.MissionNPCListener;
import com.smp.smptools.listeners.MissionTrackerListener;
import com.smp.smptools.listeners.NPCListener;
import com.smp.smptools.listeners.PortalListener;
import com.smp.smptools.listeners.PrefixGUIListener;
import com.smp.smptools.listeners.StatsGUIListener;
import com.smp.smptools.listeners.StatsListener;
import com.smp.smptools.listeners.TabHealthListener;
import com.smp.smptools.listeners.TagsGUIListener;
import com.smp.smptools.teleport.TeleportListener;
import com.smp.smptools.listeners.TrollGUIListener;
import com.smp.smptools.listeners.VaultListener;
import com.smp.smptools.listeners.AdventGUIListener;
import org.bukkit.Bukkit;

public final class ListenerRegistry {

    private ListenerRegistry() {
        // Prevent instantiation
    }

    public static void registerCoreListeners(SMPTools plugin, StatsCommand statsCommand, AdventGUIListener adventGUIListener) {
        // Core listeners
        Bukkit.getPluginManager().registerEvents(new StatsListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new JoinLeaveListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new ChatListener(plugin), plugin);

        if (plugin.getConfig().getBoolean("features.private-vault.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new VaultListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("features.stats.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new StatsGUIListener(statsCommand), plugin);
        }
        if (plugin.getConfig().getBoolean("features.homes.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new HomesGUIListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("features.prefix-color.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new PrefixGUIListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("features.leaderboard.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new LeaderboardGUIListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("features.tags.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new TagsGUIListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("features.tab-health.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new TabHealthListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("features.tpa.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new TeleportListener(plugin), plugin);
        }
        if (plugin.getConfig().getBoolean("features.advancements.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new AdvancementListener(plugin), plugin);
        }

        // Feature-gated listeners
        if (plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new ChunkLoaderListener(plugin), plugin);
        }

        if (plugin.getConfig().getBoolean("features.invsee.enabled", true)) {
            InvseeGUIListener invseeGUIListener = new InvseeGUIListener(plugin);
            plugin.setInvseeGUIListener(invseeGUIListener);
            Bukkit.getPluginManager().registerEvents(invseeGUIListener, plugin);
        }

        if (plugin.getConfig().getBoolean("features.troll.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new TrollGUIListener(plugin), plugin);
        }

        // Mission system listeners
        if (plugin.getConfig().getBoolean("features.missions.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new MissionNPCListener(plugin), plugin);
            Bukkit.getPluginManager().registerEvents(new NPCListener(plugin), plugin);
            Bukkit.getPluginManager().registerEvents(new MissionGUIListener(plugin), plugin);
            Bukkit.getPluginManager().registerEvents(new MissionTrackerListener(plugin), plugin);
        }

        // Elytra trail
        if (plugin.getConfig().getBoolean("features.elytra-trail.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new ElytraTrailListener(), plugin);
        }

        // Christmas features
        if (plugin.getConfig().getBoolean("features.christmas.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new ChristmasWorldListener(plugin), plugin);
            Bukkit.getPluginManager().registerEvents(adventGUIListener, plugin);
            Bukkit.getPluginManager().registerEvents(new PortalListener(plugin), plugin);
        }

        // Player graves
        if (plugin.getConfig().getBoolean("features.player-graves.enabled", true)) {
            Bukkit.getPluginManager().registerEvents(new com.smp.smptools.graves.GraveManager(plugin), plugin);
        }
    }
}

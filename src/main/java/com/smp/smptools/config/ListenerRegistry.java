package com.smp.smptools.config;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.StatsCommand;
import com.smp.smptools.listeners.*;
import com.smp.smptools.managers.AdventGUIListener;
import org.bukkit.Bukkit;

public final class ListenerRegistry {

    private ListenerRegistry() {
        // Prevent instantiation
    }

    public static void registerCoreListeners(SMPTools plugin, StatsCommand statsCommand, AdventGUIListener adventGUIListener) {
        // Core listeners that are always registered
        Bukkit.getPluginManager().registerEvents(new VaultListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new StatsListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new StatsGUIListener(statsCommand), plugin);
        Bukkit.getPluginManager().registerEvents(new JoinLeaveListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new ChatListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new HomesGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new PrefixGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new LeaderboardGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new TagsGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new TabHealthListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new TeleportListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new AdvancementListener(plugin), plugin);

        // Feature listeners
        Bukkit.getPluginManager().registerEvents(new ChunkLoaderListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new InvseeGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new TrollGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new MissionNPCListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new NPCListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new MissionGUIListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new ElytraTrailListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new MissionTrackerListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new ChristmasWorldListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new com.smp.smptools.graves.GraveManager(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(adventGUIListener, plugin);
        Bukkit.getPluginManager().registerEvents(new PortalListener(plugin), plugin);
    }
}

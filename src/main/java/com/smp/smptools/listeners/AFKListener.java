package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.afk.AFKManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKListener implements Listener {

    private final SMPTools plugin;
    private final AFKManager afkManager;
    private final Map<UUID, Location> lastLocationMap = new HashMap<>();

    public AFKListener(SMPTools plugin) {
        this.plugin = plugin;
        this.afkManager = plugin.getAFKManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        // Check if player actually moved position or rotated significantly
        if (from.getBlockX() != to.getBlockX() ||
            from.getBlockY() != to.getBlockY() ||
            from.getBlockZ() != to.getBlockZ() ||
            Math.abs(from.getYaw() - to.getYaw()) > 10.0f ||
            Math.abs(from.getPitch() - to.getPitch()) > 10.0f) {
            
            afkManager.updateActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> afkManager.updateActivity(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        afkManager.removePlayer(event.getPlayer());
    }
}

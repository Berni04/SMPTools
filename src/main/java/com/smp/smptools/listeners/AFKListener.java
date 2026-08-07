package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.afk.AFKManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;

public class AFKListener implements Listener {

    private final SMPTools plugin;
    private final AFKManager afkManager;

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

        if (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch()) {
            afkManager.updateActivity(player);
            return;
        }

        if (from.getWorld() != null && !from.getWorld().equals(to.getWorld())) {
            afkManager.updateActivity(player);
            return;
        }

        if (from.distanceSquared(to) > 0.000001) {
            afkManager.updateActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player != null && player.isOnline()) {
                afkManager.updateActivity(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().trim();
        String command = message.split("\\s+")[0];
        if (command.equalsIgnoreCase("/afk") || command.equalsIgnoreCase("/smptools:afk")) {
            return;
        }
        afkManager.updateActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            afkManager.updateActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            afkManager.updateActivity(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player victim) {
            afkManager.updateActivity(victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            afkManager.updateActivity(attacker);
        }
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

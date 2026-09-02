package com.smp.smptools.teleport;

import com.smp.smptools.SMPTools;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.logging.Level; // Import Level for logging

public class TeleportListener implements Listener {

    private final TeleportManager teleportManager;
    private final SMPTools plugin; // Add plugin reference for logging

    public TeleportListener(SMPTools plugin) {
        this.plugin = plugin; // Initialize plugin
        this.teleportManager = plugin.getTeleportManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        // Only log if the player is actually teleporting, to avoid spam
        if (teleportManager.isTeleporting(player)) {
            plugin.getLogger().log(Level.INFO, "PlayerMoveEvent for " + player.getName() + ". isTeleporting: true, isTeleportingSafely: " + teleportManager.isTeleportingSafely(player));
        }

        if (teleportManager.isTeleportingSafely(player)) { // Ignore movement during safe teleport
            return;
        }
        if (teleportManager.isTeleporting(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            // Check for block-level movement, allowing for head rotation
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                plugin.getLogger().log(Level.INFO, "Player " + player.getName() + " moved during teleport. Cancelling.");
                teleportManager.cancelTeleport(player, "You moved.", true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (teleportManager.isTeleporting(player)) {
                plugin.getLogger().log(Level.INFO, "EntityDamageEvent for " + player.getName() + ". isTeleporting: true. Player took damage during teleport. Cancelling.");
                teleportManager.cancelTeleport(player, "You took damage.", true);
            }
        }
    }

    @EventHandler
    public void onVehicleEnter(org.bukkit.event.vehicle.VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            if (teleportManager.isTeleporting(player)) {
                teleportManager.cancelTeleport(player, "You entered a vehicle.", true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (teleportManager.isTeleporting(player)) {
            teleportManager.cancelTeleport(player, "You died.", false);
        }
    }
}

package com.smp.smptools.teleport;

import com.smp.smptools.SMPTools;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeleportListener implements Listener {

    private final TeleportManager teleportManager;

    public TeleportListener(SMPTools plugin) {
        this.teleportManager = plugin.getTeleportManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (teleportManager.isTeleporting(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            // Check for block-level movement, allowing for head rotation
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                teleportManager.cancelTeleport(player, "You moved.", true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (teleportManager.isTeleporting(player)) {
                teleportManager.cancelTeleport(player, "You took damage.", true);
            }
        }
    }
}

package com.smp.smptools.teleport;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level; // Import Level for logging

public class TeleportManager {

    private final SMPTools plugin;
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Location> initialLocations = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> teleportingSafely = ConcurrentHashMap.newKeySet(); // New set to track safe teleports

    public TeleportManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public boolean isTeleporting(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    public void startTeleport(Player player, Location location, String destinationName) {
        plugin.getLogger().log(Level.INFO, "startTeleport called for " + player.getName() + " to " + destinationName);
        if (isTeleporting(player)) {
            plugin.getLogger().log(Level.INFO, "Player " + player.getName() + " already teleporting, cancelling current.");
            cancelTeleport(player, "You started a new teleport.", false);
        }

        player.sendMessage(ChatColor.GREEN + "Teleporting to " + destinationName + " in 3 seconds... Don't move or take damage!");
        initialLocations.put(player.getUniqueId(), player.getLocation());
        plugin.getLogger().log(Level.INFO, "Initial location for " + player.getName() + ": " + player.getLocation());

        BukkitTask task = new BukkitRunnable() {
            private int countdown = 3;

            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendMessage(ChatColor.GRAY + "Teleporting in " + countdown + "...");
                    plugin.getLogger().log(Level.INFO, "Teleport countdown for " + player.getName() + ": " + countdown);
                    countdown--;
                } else {
                    plugin.getLogger().log(Level.INFO, "Teleport countdown finished for " + player.getName() + ". Performing teleport.");
                    teleportingSafely.add(player.getUniqueId()); // Mark player as safely teleporting
                    player.teleport(location);
                    plugin.getLogger().log(Level.INFO, "Player " + player.getName() + " teleported to " + location);
                    teleportingSafely.remove(player.getUniqueId()); // Unmark after teleport
                    player.sendMessage(ChatColor.GREEN + "Teleport successful!");
                    finishTeleport(player);
                    this.cancel(); // Ensure the runnable stops after teleporting
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second

        pendingTeleports.put(player.getUniqueId(), task);
        plugin.getLogger().log(Level.INFO, "Teleport task started for " + player.getName());
    }

    public void cancelTeleport(Player player, String reason, boolean showMessage) {
        plugin.getLogger().log(Level.INFO, "cancelTeleport called for " + player.getName() + " with reason: " + reason);
        if (isTeleporting(player)) {
            pendingTeleports.get(player.getUniqueId()).cancel();
            plugin.getLogger().log(Level.INFO, "Teleport task cancelled for " + player.getName());
            if (showMessage) {
                player.sendMessage(ChatColor.RED + "Teleport cancelled: " + reason);
            }
            finishTeleport(player);
        } else {
            plugin.getLogger().log(Level.INFO, "cancelTeleport called for " + player.getName() + " but not currently teleporting.");
        }
    }

    private void finishTeleport(Player player) {
        plugin.getLogger().log(Level.INFO, "finishTeleport called for " + player.getName());
        pendingTeleports.remove(player.getUniqueId());
        initialLocations.remove(player.getUniqueId());
        teleportingSafely.remove(player.getUniqueId()); // Ensure player is removed from safe teleporting set
        plugin.getLogger().log(Level.INFO, "Teleport state cleared for " + player.getName());
    }

    public Location getInitialLocation(Player player) {
        return initialLocations.get(player.getUniqueId());
    }

    public boolean isTeleportingSafely(Player player) {
        return teleportingSafely.contains(player.getUniqueId());
    }
}

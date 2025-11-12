package com.smp.smptools.teleport;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final SMPTools plugin;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();
    private final Map<UUID, Location> initialLocations = new HashMap<>();

    public TeleportManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public boolean isTeleporting(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    public void startTeleport(Player player, Location location, String destinationName) {
        if (isTeleporting(player)) {
            cancelTeleport(player, "You started a new teleport.", false);
        }

        player.sendMessage(ChatColor.GREEN + "Teleporting to " + destinationName + " in 3 seconds... Don't move or take damage!");
        initialLocations.put(player.getUniqueId(), player.getLocation());

        BukkitTask task = new BukkitRunnable() {
            private int countdown = 3;

            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendMessage(ChatColor.GRAY + "Teleporting in " + countdown + "...");
                    countdown--;
                } else {
                    player.teleport(location);
                    player.sendMessage(ChatColor.GREEN + "Teleport successful!");
                    finishTeleport(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second

        pendingTeleports.put(player.getUniqueId(), task);
    }

    public void cancelTeleport(Player player, String reason, boolean showMessage) {
        if (isTeleporting(player)) {
            pendingTeleports.get(player.getUniqueId()).cancel();
            if (showMessage) {
                player.sendMessage(ChatColor.RED + "Teleport cancelled: " + reason);
            }
            finishTeleport(player);
        }
    }

    private void finishTeleport(Player player) {
        pendingTeleports.remove(player.getUniqueId());
        initialLocations.remove(player.getUniqueId());
    }

    public Location getInitialLocation(Player player) {
        return initialLocations.get(player.getUniqueId());
    }
}

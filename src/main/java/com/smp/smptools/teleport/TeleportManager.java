package com.smp.smptools.teleport;

import com.smp.smptools.SMPTools;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private final SMPTools plugin;
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Location> initialLocations = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> teleportingSafely = ConcurrentHashMap.newKeySet();

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

        player.sendMessage(plugin.getMessageManager().getMessage("teleport.starting", player,
                Map.of("destination", destinationName)));
        initialLocations.put(player.getUniqueId(), player.getLocation());

        BukkitTask task = new BukkitRunnable() {
            private int countdown = 3;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    finishTeleport(player);
                    this.cancel();
                    return;
                }
                if (countdown > 0) {
                    player.sendMessage(plugin.getMessageManager().getMessage("teleport.countdown", player,
                            Map.of("seconds", String.valueOf(countdown))));
                    countdown--;
                } else {
                    teleportingSafely.add(player.getUniqueId());
                    player.teleport(location);
                    teleportingSafely.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        player.sendMessage(plugin.getMessageManager().getMessage("teleport.successful"));
                    }
                    finishTeleport(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        pendingTeleports.put(player.getUniqueId(), task);
    }

    public void cancelTeleport(Player player, String reason, boolean showMessage) {
        if (isTeleporting(player)) {
            pendingTeleports.get(player.getUniqueId()).cancel();
            if (showMessage && player.isOnline()) {
                player.sendMessage(plugin.getMessageManager().getMessage("teleport.cancelled", player,
                        Map.of("reason", reason)));
            }
            finishTeleport(player);
        }
    }

    private void finishTeleport(Player player) {
        pendingTeleports.remove(player.getUniqueId());
        initialLocations.remove(player.getUniqueId());
        teleportingSafely.remove(player.getUniqueId());
    }

    public Location getInitialLocation(Player player) {
        return initialLocations.get(player.getUniqueId());
    }

    public boolean isTeleportingSafely(Player player) {
        return teleportingSafely.contains(player.getUniqueId());
    }
}

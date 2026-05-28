package com.smp.smptools.teleport;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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
        plugin.getLogger().log(Level.INFO, "startTeleport called for " + player.getName() + " to " + destinationName);
        if (isTeleporting(player)) {
            plugin.getLogger().log(Level.INFO, "Player " + player.getName() + " already teleporting, cancelling current.");
            cancelTeleport(player, "You started a new teleport.", false);
        }

        player.sendMessage(Component.text("Teleporting to " + destinationName + " in 3 seconds... Don't move or take damage!", NamedTextColor.GREEN));
        initialLocations.put(player.getUniqueId(), player.getLocation());
        plugin.getLogger().log(Level.INFO, "Initial location for " + player.getName() + ": " + player.getLocation());

        BukkitTask task = new BukkitRunnable() {
            private int countdown = 3;

            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendMessage(Component.text("Teleporting in " + countdown + "...", NamedTextColor.GRAY));
                    plugin.getLogger().log(Level.INFO, "Teleport countdown for " + player.getName() + ": " + countdown);
                    countdown--;
                } else {
                    plugin.getLogger().log(Level.INFO, "Teleport countdown finished for " + player.getName() + ". Performing teleport.");
                    teleportingSafely.add(player.getUniqueId());
                    player.teleport(location);
                    plugin.getLogger().log(Level.INFO, "Player " + player.getName() + " teleported to " + location);
                    teleportingSafely.remove(player.getUniqueId());
                    player.sendMessage(Component.text("Teleport successful!", NamedTextColor.GREEN));
                    finishTeleport(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        pendingTeleports.put(player.getUniqueId(), task);
        plugin.getLogger().log(Level.INFO, "Teleport task started for " + player.getName());
    }

    public void cancelTeleport(Player player, String reason, boolean showMessage) {
        plugin.getLogger().log(Level.INFO, "cancelTeleport called for " + player.getName() + " with reason: " + reason);
        if (isTeleporting(player)) {
            pendingTeleports.get(player.getUniqueId()).cancel();
            plugin.getLogger().log(Level.INFO, "Teleport task cancelled for " + player.getName());
            if (showMessage) {
                player.sendMessage(Component.text("Teleport cancelled: " + reason, NamedTextColor.RED));
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
        teleportingSafely.remove(player.getUniqueId());
        plugin.getLogger().log(Level.INFO, "Teleport state cleared for " + player.getName());
    }

    public Location getInitialLocation(Player player) {
        return initialLocations.get(player.getUniqueId());
    }

    public boolean isTeleportingSafely(Player player) {
        return teleportingSafely.contains(player.getUniqueId());
    }
}

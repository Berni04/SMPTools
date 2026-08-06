package com.smp.smptools.trails;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrailManager {

    private final SMPTools plugin;
    private final Map<UUID, TrailType> activeTrails = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> explicitlySet = ConcurrentHashMap.newKeySet();
    private int taskId = -1;

    public TrailManager(SMPTools plugin) {
        this.plugin = plugin;
        startTrailTask();
    }

    public void startTrailTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        // Run trail particle spawner every 3 ticks
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!plugin.getConfig().getBoolean("features.trails.enabled", true)) {
                return;
            }

            for (Map.Entry<UUID, TrailType> entry : activeTrails.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline() && !player.isDead()) {
                    TrailType trail = entry.getValue();
                    if (!player.hasPermission("smptools.trails.all") && !player.hasPermission(trail.getPermission())) {
                        continue;
                    }
                    player.getWorld().spawnParticle(
                            trail.getParticle(),
                            player.getLocation().add(0, 0.2, 0),
                            3, 0.15, 0.15, 0.15, 0.02
                    );
                }
            }
        }, 3L, 3L);
    }

    public TrailType getActiveTrail(Player player) {
        if (player == null) return null;
        return activeTrails.get(player.getUniqueId());
    }

    public boolean hasExplicitlySet(Player player) {
        return player != null && explicitlySet.contains(player.getUniqueId());
    }

    public void setTrail(Player player, TrailType trail) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        explicitlySet.add(uuid);
        if (trail == null) {
            activeTrails.remove(uuid);
            if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
                plugin.getStorageManager().getProvider().saveStat(uuid, "active_trail", "");
            }
        } else {
            activeTrails.put(uuid, trail);
            if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
                plugin.getStorageManager().getProvider().saveStat(uuid, "active_trail", trail.getId());
            }
        }
    }

    public void loadPlayerTrail(Player player) {
        if (player == null || !player.isOnline() || hasExplicitlySet(player)) return;
        if (plugin.getStorageManager() == null || plugin.getStorageManager().getProvider() == null) return;
        Object val = plugin.getStorageManager().getProvider().getStat(player.getUniqueId(), "active_trail", null);
        if (val != null && !val.toString().isEmpty()) {
            if (!player.isOnline() || hasExplicitlySet(player)) return;
            TrailType trail = TrailType.fromId(val.toString());
            if (trail != null) {
                activeTrails.put(player.getUniqueId(), trail);
            }
        }
    }

    public void removePlayer(Player player) {
        if (player != null) {
            activeTrails.remove(player.getUniqueId());
            explicitlySet.remove(player.getUniqueId());
        }
    }

    public Map<UUID, TrailType> getActiveTrails() {
        return activeTrails;
    }
}

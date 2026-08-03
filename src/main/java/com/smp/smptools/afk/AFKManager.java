package com.smp.smptools.afk;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKManager {

    private final SMPTools plugin;
    private final Map<UUID, Long> lastActivityMap = new ConcurrentHashMap<>();
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private int taskId = -1;

    public AFKManager(SMPTools plugin) {
        this.plugin = plugin;
        startAFKCheckTask();
    }

    public void startAFKCheckTask() {
        if (plugin == null) return;
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        // Run check task every 10 seconds (200 ticks)
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!plugin.getConfig().getBoolean("features.afk.enabled", true)) {
                return;
            }

            long timeoutMs = plugin.getConfig().getLong("features.afk.timeout-minutes", 30) * 60 * 1000L;
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                long lastActivity = lastActivityMap.getOrDefault(uuid, now);

                if (!isAFK(player) && (now - lastActivity) >= timeoutMs) {
                    setAFK(player, true, true);
                }
            }
        }, 200L, 200L);
    }

    public boolean isAFK(Player player) {
        return player != null && afkPlayers.contains(player.getUniqueId());
    }

    public boolean isAFK(UUID uuid) {
        return afkPlayers.contains(uuid);
    }

    public void updateActivity(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        lastActivityMap.put(uuid, System.currentTimeMillis());

        if (isAFK(player)) {
            setAFK(player, false, true);
        }
    }

    public void setAFK(Player player, boolean afk, boolean announce) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        if (afk) {
            if (afkPlayers.add(uuid)) {
                if (announce) {
                    Bukkit.broadcast(plugin.getMessageManager().getMessage("afk.now-afk", player));
                }
                plugin.getNameTagListener().updatePlayerName(player);
            }
        } else {
            if (afkPlayers.remove(uuid)) {
                lastActivityMap.put(uuid, System.currentTimeMillis());
                if (announce) {
                    Bukkit.broadcast(plugin.getMessageManager().getMessage("afk.no-longer-afk", player));
                }
                plugin.getNameTagListener().updatePlayerName(player);
            }
        }
    }

    public void removePlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        lastActivityMap.remove(uuid);
        afkPlayers.remove(uuid);
    }

    public Set<UUID> getAfkPlayers() {
        return afkPlayers;
    }
}

package com.smp.smptools.events;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.minievents.MiniEventListener;
import com.smp.smptools.events.minievents.MiniEventSession;
import com.smp.smptools.events.minievents.MiniEventType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Core event management engine for SMPTools.
 * Manages automated event timers, active event state, event listeners, and batched offline reward delivery.
 */
public class EventManager {

    private final SMPTools plugin;
    private MiniEventSession activeSession;
    private BukkitTask autoScheduleTask;
    private final Random random = new Random();

    private File dataFile;
    private FileConfiguration dataConfig;

    public EventManager(SMPTools plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "events_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create events_data.yml", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private synchronized void saveData() {
        if (dataConfig == null) return;
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save events_data.yml", e);
        }
    }

    public synchronized void queueOfflineRewardsBatch(Map<UUID, List<String>> rewardsByPlayer) {
        if (rewardsByPlayer == null || rewardsByPlayer.isEmpty()) return;

        for (Map.Entry<UUID, List<String>> entry : rewardsByPlayer.entrySet()) {
            String path = "pending_rewards." + entry.getKey().toString();
            List<String> current = new ArrayList<>(dataConfig.getStringList(path));
            current.addAll(entry.getValue());
            dataConfig.set(path, current);
        }
        saveData();
    }

    public synchronized void queueOfflineReward(UUID uuid, String rewardStr) {
        List<String> list = dataConfig.getStringList("pending_rewards." + uuid.toString());
        list.add(rewardStr);
        dataConfig.set("pending_rewards." + uuid.toString(), list);
        saveData();
    }

    public synchronized void claimOfflineRewards(Player player) {
        String path = "pending_rewards." + player.getUniqueId().toString();
        List<String> pending = new ArrayList<>(dataConfig.getStringList(path));
        if (!pending.isEmpty()) {
            List<String> remaining = new ArrayList<>();
            boolean anyDelivered = false;

            for (String reward : pending) {
                boolean ok = executeReward(player, reward);
                if (ok) {
                    anyDelivered = true;
                } else {
                    remaining.add(reward);
                }
            }

            if (remaining.isEmpty()) {
                dataConfig.set(path, null);
            } else {
                dataConfig.set(path, remaining);
            }
            saveData();

            if (anyDelivered) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><b>[EVENT]</b></gold> <yellow>You received event rewards earned while offline!</yellow>"));
            }
        }
    }

    public boolean executeReward(Player player, String rewardStr) {
        try {
            if (rewardStr.startsWith("cmd:")) {
                String cmd = rewardStr.substring(4).replace("%player%", player.getName());
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else if (rewardStr.startsWith("item:")) {
                String[] parts = rewardStr.substring(5).trim().split("\\s+");
                Material mat = Material.matchMaterial(parts[0].toUpperCase());
                int amount = 1;
                if (parts.length > 1) {
                    try {
                        amount = Math.max(1, Integer.parseInt(parts[1]));
                    } catch (NumberFormatException ignored) {}
                }
                if (mat != null) {
                    Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(mat, amount));
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    return true;
                } else {
                    plugin.getLogger().warning("Unknown material in reward '" + rewardStr + "' for " + player.getName());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error delivering reward '" + rewardStr + "' to " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public void initialize() {
        // Register listener
        Bukkit.getPluginManager().registerEvents(new MiniEventListener(plugin, this), plugin);

        // Start automated scheduler if enabled
        if (plugin.getEventsConfig().getBoolean("events.enabled", true)) {
            int intervalMinutes = plugin.getEventsConfig().getInt("events.interval-minutes", 120);
            long periodTicks = intervalMinutes * 60 * 20L;

            autoScheduleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (activeSession == null || !activeSession.isActive()) {
                        java.util.List<MiniEventType> enabledTypes = new java.util.ArrayList<>();
                        for (MiniEventType t : MiniEventType.values()) {
                            if (plugin.getEventsConfig().getBoolean("events.types." + t.getConfigKey() + ".enabled", true)) {
                                enabledTypes.add(t);
                            }
                        }
                        if (!enabledTypes.isEmpty()) {
                            MiniEventType randomType = enabledTypes.get(random.nextInt(enabledTypes.size()));
                            int duration = Math.max(1, plugin.getEventsConfig().getInt("events.types." + randomType.getConfigKey() + ".duration-minutes", 15));
                            startEvent(randomType, duration);
                        }
                    }
                }
            }.runTaskTimer(plugin, periodTicks, periodTicks);
        }
    }

    public boolean startEvent(MiniEventType type, int durationMinutes) {
        if (activeSession != null && activeSession.isActive()) {
            return false;
        }

        activeSession = new MiniEventSession(plugin, this, type, durationMinutes);
        activeSession.start();
        return true;
    }

    public boolean stopActiveEvent() {
        if (activeSession == null || !activeSession.isActive()) {
            return false;
        }

        activeSession.end();
        activeSession = null;
        return true;
    }

    public MiniEventSession getActiveSession() {
        return activeSession;
    }

    public void shutdown() {
        if (autoScheduleTask != null) {
            autoScheduleTask.cancel();
        }
        if (activeSession != null && activeSession.isActive()) {
            activeSession.end();
        }
    }
}

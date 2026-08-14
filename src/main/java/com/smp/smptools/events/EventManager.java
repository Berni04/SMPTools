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

    private synchronized boolean saveData() {
        if (dataConfig == null) return false;
        try {
            dataConfig.save(dataFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save events_data.yml", e);
            return false;
        }
    }

    public synchronized boolean queueOfflineRewardsBatch(Map<UUID, List<String>> rewardsByPlayer) {
        if (rewardsByPlayer == null || rewardsByPlayer.isEmpty()) return true;

        for (Map.Entry<UUID, List<String>> entry : rewardsByPlayer.entrySet()) {
            String path = "pending_rewards." + entry.getKey().toString();
            List<String> current = new ArrayList<>(dataConfig.getStringList(path));
            current.addAll(entry.getValue());
            dataConfig.set(path, current);
        }
        boolean saved = saveData();
        if (!saved && plugin != null) {
            plugin.getLogger().severe("Failed to persist offline rewards batch in events_data.yml!");
        }
        return saved;
    }

    public enum RewardType {
        COMMAND,
        ITEM
    }

    public static class ParsedEventReward {
        private final RewardType type;
        private final String command;
        private final Material material;
        private final int amount;
        private final int retryCount;

        public ParsedEventReward(RewardType type, String command, Material material, int amount, int retryCount) {
            this.type = type;
            this.command = command;
            this.material = material;
            this.amount = amount;
            this.retryCount = retryCount;
        }

        public RewardType getType() { return type; }
        public String getCommand() { return command; }
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public int getRetryCount() { return retryCount; }
    }

    private static final java.util.regex.Pattern RETRY_PATTERN = java.util.regex.Pattern.compile("^(.*)#retry:(.*)$");

    public static ParsedEventReward parseRewardString(String rewardStr) {
        if (rewardStr == null || rewardStr.isBlank()) return null;
        int retryCount = 0;
        String raw = rewardStr.trim();
        java.util.regex.Matcher matcher = RETRY_PATTERN.matcher(raw);
        if (matcher.matches()) {
            String suffix = matcher.group(2).trim();
            if (suffix.isEmpty()) {
                return null;
            }
            try {
                retryCount = Integer.parseInt(suffix);
                if (retryCount < 0) {
                    return null;
                }
                raw = matcher.group(1).trim();
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (raw.startsWith("cmd:")) {
            String cmd = raw.substring(4).trim();
            if (cmd.isBlank()) return null;
            return new ParsedEventReward(RewardType.COMMAND, cmd, null, 0, retryCount);
        } else if (raw.startsWith("item:")) {
            String[] parts = raw.substring(5).trim().split("\\s+");
            if (parts.length == 0 || parts[0].isBlank()) return null;
            Material mat = Material.matchMaterial(parts[0].toUpperCase());
            if (mat == null) return null;
            int amount = 1;
            if (parts.length > 1) {
                try {
                    amount = Integer.parseInt(parts[1]);
                    if (amount <= 0) {
                        return null;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return new ParsedEventReward(RewardType.ITEM, null, mat, amount, retryCount);
        }
        return null;
    }

    public synchronized void claimOfflineRewards(Player player) {
        String path = "pending_rewards." + player.getUniqueId().toString();
        List<String> pending = new ArrayList<>(dataConfig.getStringList(path));
        if (pending.isEmpty()) return;

        boolean anyDelivered = false;
        List<String> remaining = new ArrayList<>(pending);

        for (String reward : pending) {
            List<String> snapshotBefore = new ArrayList<>(remaining);
            ParsedEventReward parsed = parseRewardString(reward);
            if (parsed == null) {
                plugin.getLogger().warning("Discarding unparseable/malformed offline reward '" + reward + "' for " + player.getName());
                remaining.remove(reward);
                dataConfig.set(path, remaining.isEmpty() ? null : remaining);
                if (!saveData()) {
                    dataConfig.set(path, snapshotBefore);
                    break;
                }
                continue;
            }

            // Persist removal from queue BEFORE executing reward to prevent duplication on crash or disk save failure
            remaining.remove(reward);
            dataConfig.set(path, remaining.isEmpty() ? null : remaining);
            if (!saveData()) {
                plugin.getLogger().severe("Failed to persist offline reward removal for " + player.getName() + ", aborting claim execution.");
                dataConfig.set(path, snapshotBefore);
                break;
            }

            boolean ok = executeReward(player, parsed);
            if (ok) {
                anyDelivered = true;
            } else {
                int nextRetry = parsed.getRetryCount() + 1;
                if (nextRetry >= 3) {
                    plugin.getLogger().severe("Permanently dropping unexecutable offline reward '" + reward + "' for " + player.getName() + " after 3 failed attempts.");
                } else {
                    String baseCommandOrItem = parsed.getType() == RewardType.COMMAND ? "cmd:" + parsed.getCommand() : "item:" + parsed.getMaterial().name() + " " + parsed.getAmount();
                    remaining.add(baseCommandOrItem + "#retry:" + nextRetry);
                    plugin.getLogger().warning("Transient delivery failure for offline reward '" + reward + "' for " + player.getName() + " (attempt " + nextRetry + "/3), retaining for retry.");
                    dataConfig.set(path, remaining.isEmpty() ? null : remaining);
                    if (!saveData()) {
                        plugin.getLogger().severe("Failed to persist retry state for offline reward for " + player.getName() + ", aborting remaining queue.");
                        break;
                    }
                }
            }
        }

        if (anyDelivered) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gold><b>[EVENT]</b></gold> <yellow>You received event rewards earned while offline!</yellow>"));
        }
    }

    public boolean executeReward(Player player, ParsedEventReward parsed) {
        if (player == null || parsed == null) return false;
        try {
            if (parsed.getType() == RewardType.COMMAND) {
                String cmd = parsed.getCommand().replace("%player%", player.getName());
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else if (parsed.getType() == RewardType.ITEM) {
                ItemStack item = new ItemStack(parsed.getMaterial(), parsed.getAmount());
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                return true;
            }
        } catch (Exception e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to execute reward for " + player.getName() + ": " + e.getMessage());
            }
        }
        return false;
    }

    public boolean executeReward(Player player, String rewardStr) {
        if (player == null) return false;
        ParsedEventReward parsed = parseRewardString(rewardStr);
        if (parsed == null) {
            if (plugin != null) {
                plugin.getLogger().warning("Unparseable reward '" + rewardStr + "' for " + player.getName());
            }
            return false;
        }
        return executeReward(player, parsed);
    }

    public void initialize() {
        // Register listener
        Bukkit.getPluginManager().registerEvents(new MiniEventListener(plugin, this), plugin);

        // Start automated scheduler if enabled
        if (plugin.getEventsConfig().getBoolean("events.enabled", true)) {
            int intervalMinutes = plugin.getEventsConfig().getInt("events.interval-minutes", 120);
            if (intervalMinutes < 1) {
                plugin.getLogger().warning("Mini-events interval is configured below 1 minute (" + intervalMinutes + "m). Clamping to 1 minute.");
                intervalMinutes = 1;
            }
            long periodTicks = Math.max(1200L, intervalMinutes * 60L * 20L);

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

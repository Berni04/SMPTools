package com.smp.smptools.events.minievents;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Tracks and manages a live active mini-event session.
 * Handles timers, score maps, combo streaks, BossBar HUD, Scoreboard sidebar, and reward delivery.
 */
public class MiniEventSession {

    private final SMPTools plugin;
    private final MiniEventType type;
    private final int totalDurationSeconds;
    private int remainingSeconds;

    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final Map<UUID, Integer> comboCounts = new HashMap<>();
    private final Map<UUID, Long> lastActionTimes = new HashMap<>();
    private final Map<UUID, Scoreboard> originalScoreboards = new HashMap<>();

    private BossBar bossBar;
    private BukkitTask timerTask;
    private boolean active = false;

    public MiniEventSession(SMPTools plugin, MiniEventType type, int durationMinutes) {
        this.plugin = plugin;
        this.type = type;
        this.totalDurationSeconds = durationMinutes * 60;
        this.remainingSeconds = totalDurationSeconds;
    }

    public void start() {
        this.active = true;

        // Initialize BossBar if enabled
        if (plugin.getEventsConfig().getBoolean("events.hud.bossbar", true)) {
            String colorStr = plugin.getEventsConfig().getString("events.hud.bossbar-color", "YELLOW").toUpperCase();
            BossBar.Color color;
            try {
                color = BossBar.Color.valueOf(colorStr);
            } catch (IllegalArgumentException e) {
                color = BossBar.Color.YELLOW;
            }

            bossBar = BossBar.bossBar(
                    Component.text(type.getFormattedName() + " | Time: " + formatTime(remainingSeconds), NamedTextColor.GOLD, TextDecoration.BOLD),
                    1.0f,
                    color,
                    BossBar.Overlay.PROGRESS
            );

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showBossBar(bossBar);
            }
        }

        // Announce Event Start
        Component startNotice = MiniMessage.miniMessage().deserialize(
                "<gold><b>[EVENT]</b></gold> <yellow>The <b>" + type.getFormattedName() + "</b> has started! Duration: " + (totalDurationSeconds / 60) + "m!</yellow>"
        );
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(startNotice);
            p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.2f);
        }

        // Start Timer Task
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                remainingSeconds--;

                if (remainingSeconds <= 0) {
                    end();
                    cancel();
                    return;
                }

                updateHUD();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void addPoints(Player player, int basePoints, String reason) {
        if (!active || player == null) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTime = lastActionTimes.getOrDefault(uuid, 0L);
        FileConfiguration cfg = plugin.getEventsConfig();

        boolean comboEnabled = cfg.getBoolean("events.types." + type.getConfigKey() + ".combo.enabled", true);
        double maxMultiplier = cfg.getDouble("events.types." + type.getConfigKey() + ".combo.max-multiplier", 2.0);

        int currentCombo = comboCounts.getOrDefault(uuid, 0);
        if (now - lastTime < 10000) {
            currentCombo++;
        } else {
            currentCombo = 1;
        }

        comboCounts.put(uuid, currentCombo);
        lastActionTimes.put(uuid, now);

        double multiplier = 1.0;
        if (comboEnabled) {
            multiplier = Math.min(maxMultiplier, 1.0 + ((currentCombo - 1) * 0.1));
        }

        int earnedPoints = (int) Math.round(basePoints * multiplier);

        int currentScore = playerScores.getOrDefault(uuid, 0);
        int newScore = currentScore + earnedPoints;
        playerScores.put(uuid, newScore);

        // Action Bar Notification
        String actionMsg = "<gold><b>+" + earnedPoints + " Event Pts!</b></gold> <gray>(" + reason + ")</gray>";
        if (comboEnabled && currentCombo > 1) {
            actionMsg += " <yellow><b>[" + currentCombo + "x Combo (" + String.format("%.1f", multiplier) + "x)]</b></yellow>";
        }
        player.sendActionBar(MiniMessage.miniMessage().deserialize(actionMsg));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);

        updateHUD();
    }

    private void updateHUD() {
        if (bossBar != null) {
            float progress = Math.max(0.0f, (float) remainingSeconds / totalDurationSeconds);
            bossBar.progress(progress);
            bossBar.name(Component.text(type.getFormattedName() + " | Time: " + formatTime(remainingSeconds), NamedTextColor.GOLD, TextDecoration.BOLD));

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showBossBar(bossBar);
            }
        }

        // Render Scoreboard Sidebar if enabled
        if (plugin.getEventsConfig().getBoolean("events.hud.scoreboard", true)) {
            renderSidebar();
        }
    }

    private void renderSidebar() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        List<Map.Entry<UUID, Integer>> sorted = getTopPlayers(5);

        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            if (board == manager.getMainScoreboard()) {
                if (!originalScoreboards.containsKey(p.getUniqueId())) {
                    originalScoreboards.put(p.getUniqueId(), board);
                }
                board = manager.getNewScoreboard();
                p.setScoreboard(board);
            }

            Objective obj = board.getObjective("smpevent");
            if (obj != null) {
                obj.unregister();
            }

            obj = board.registerNewObjective("smpevent", Criteria.DUMMY, Component.text("🏆 " + type.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            Score timeScore = obj.getScore("⏳ Time: " + formatTime(remainingSeconds));
            timeScore.setScore(10);

            Score blank = obj.getScore(" ");
            blank.setScore(9);

            int rankIndex = 8;
            for (int i = 0; i < sorted.size(); i++) {
                Map.Entry<UUID, Integer> entry = sorted.get(i);
                String pName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (pName == null) pName = "Player";
                Score s = obj.getScore("#" + (i + 1) + " " + pName + ": " + entry.getValue() + "pts");
                s.setScore(rankIndex--);
            }

            int myScore = playerScores.getOrDefault(p.getUniqueId(), 0);
            Score myS = obj.getScore("Your Points: " + myScore);
            myS.setScore(2);
        }
    }

    public void end() {
        if (!active) return;
        this.active = false;

        if (timerTask != null) {
            timerTask.cancel();
        }

        if (bossBar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bossBar);
            }
            bossBar = null;
        }

        // Restore original scoreboards
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Scoreboard orig = originalScoreboards.get(p.getUniqueId());
                if (orig != null) {
                    p.setScoreboard(orig);
                } else {
                    Scoreboard current = p.getScoreboard();
                    Objective obj = current.getObjective("smpevent");
                    if (obj != null) {
                        obj.unregister();
                    }
                }
            }
        }
        originalScoreboards.clear();

        // Podium reveal broadcast
        List<Map.Entry<UUID, Integer>> top3 = getTopPlayers(3);
        Component header = MiniMessage.miniMessage().deserialize(
                "\n<gold>====================================</gold>\n" +
                "<yellow><b>🏆 " + type.getDisplayName() + " Has Ended! 🏆</b></yellow>\n" +
                "<gold>====================================</gold>"
        );
        Bukkit.broadcast(header);

        if (top3.isEmpty()) {
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<gray>No players scored points during this event.</gray>\n"));
        } else {
            for (int i = 0; i < top3.size(); i++) {
                Map.Entry<UUID, Integer> entry = top3.get(i);
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = "Unknown";
                Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                        "<gold>#" + (i + 1) + "</gold> <yellow><b>" + name + "</b></yellow> - <green>" + entry.getValue() + " Points</green>"
                ));
            }
        }
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<gold>====================================</gold>\n"));

        // Deliver Rewards
        deliverRewards(top3);
    }

    private void deliverRewards(List<Map.Entry<UUID, Integer>> top3) {
        FileConfiguration cfg = plugin.getEventsConfig();

        // 1st, 2nd, 3rd place rewards
        String[] keys = new String[]{"1st_place", "2nd_place", "3rd_place"};
        for (int i = 0; i < top3.size() && i < keys.length; i++) {
            Player winner = Bukkit.getPlayer(top3.get(i).getKey());
            if (winner != null && winner.isOnline()) {
                List<String> rewards = cfg.getStringList("events.rewards." + keys[i]);
                for (String reward : rewards) {
                    executeReward(winner, reward);
                }
            }
        }

        // Participation rewards
        List<String> partRewards = cfg.getStringList("events.rewards.participation");
        if (!partRewards.isEmpty()) {
            for (UUID uuid : playerScores.keySet()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline() && playerScores.get(uuid) > 0) {
                    for (String reward : partRewards) {
                        executeReward(p, reward);
                    }
                }
            }
        }
    }

    private void executeReward(Player player, String rewardStr) {
        if (rewardStr.startsWith("cmd:")) {
            String cmd = rewardStr.substring(4).replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (rewardStr.startsWith("item:")) {
            String[] parts = rewardStr.substring(5).split(" ");
            Material mat = Material.matchMaterial(parts[0].toUpperCase());
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            if (mat != null) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(mat, amount));
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }

    public List<Map.Entry<UUID, Integer>> getTopPlayers(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(playerScores.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public boolean isActive() {
        return active;
    }

    public MiniEventType getType() {
        return type;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getPlayerScore(UUID uuid) {
        return playerScores.getOrDefault(uuid, 0);
    }
}

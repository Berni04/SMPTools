package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.leaderboard.LeaderboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LeaderboardCommand implements CommandExecutor {

    private final SMPTools plugin;
    private final List<String> validStats = Arrays.asList("blocks_broken", "blocks_placed", "playtime", "deaths", "player_kills");

    public LeaderboardCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String statType = "blocks_broken"; // Default stat
        if (args.length > 0) {
            if (validStats.contains(args[0].toLowerCase())) {
                statType = args[0].toLowerCase();
            } else {
                sender.sendMessage(Component.text("Invalid stat type. Valid types are: " + String.join(", ", validStats), NamedTextColor.RED));
                return true;
            }
        }

        LeaderboardManager manager = plugin.getLeaderboardManager();
        Map<String, Long> leaderboard = manager.getLeaderboard(statType);

        if (leaderboard.isEmpty()) {
            sender.sendMessage(Component.text("No leaderboard data available for this stat.", NamedTextColor.YELLOW));
            return true;
        }

        String title = statType.replace('_', ' ').toUpperCase();
        sender.sendMessage(Component.text("--- Top 10 Players for " + title + " ---", NamedTextColor.GOLD));

        final String finalStatType = statType;
        AtomicInteger rank = new AtomicInteger(1);
        leaderboard.forEach((playerName, score) -> {
            Component message = Component.text(rank.getAndIncrement() + ". ", NamedTextColor.GRAY)
                    .append(Component.text(playerName, NamedTextColor.AQUA))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(formatScore(finalStatType, score), NamedTextColor.YELLOW));
            sender.sendMessage(message);
        });

        return true;
    }

    private String formatScore(String statType, long score) {
        if (statType.equals("playtime")) {
            long days = score / 1440;
            long hours = (score % 1440) / 60;
            long minutes = score % 60;
            return String.format("%dD %dH %dM", days, hours, minutes);
        }
        return String.valueOf(score);
    }
}
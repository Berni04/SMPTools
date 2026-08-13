package com.smp.smptools.events.seasonal.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.seasonal.SeasonType;
import com.smp.smptools.events.seasonal.SeasonalManager;
import com.smp.smptools.events.seasonal.gui.SeasonalGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command handler for /seasonal.
 */
public class SeasonalCommand implements CommandExecutor, TabCompleter {

    private final SMPTools plugin;
    private final SeasonalManager seasonalManager;
    private final SeasonalGUI seasonalGUI;

    public SeasonalCommand(SMPTools plugin, SeasonalManager seasonalManager, SeasonalGUI seasonalGUI) {
        this.plugin = plugin;
        this.seasonalManager = seasonalManager;
        this.seasonalGUI = seasonalGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                seasonalGUI.openGUI(player);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can open the Seasonal Events Hub.</red>"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (!sender.hasPermission("smptools.seasonal.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to change active season.</red>"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /seasonal start <season_type></red>"));
                return true;
            }

            try {
                SeasonType type = SeasonType.valueOf(args[1].toUpperCase());
                seasonalManager.setForcedSeason(type);
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Successfully forced season to: </green>" + type.getFormattedName()));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid season. Available: " + Arrays.toString(SeasonType.values()) + "</red>"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("smptools.seasonal.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to reset season.</red>"));
                return true;
            }

            seasonalManager.setForcedSeason(null);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reset season to automatic calendar date detection. Active: " + seasonalManager.getCurrentSeason().getFormattedName() + "</green>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            SeasonType current = seasonalManager.getCurrentSeason();
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<gold>Current Season: " + current.getFormattedName() + "</gold>"));
            return true;
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand. Use /seasonal, /seasonal start <type>, /seasonal reset, or /seasonal status.</red>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("start");
            completions.add("reset");
            completions.add("status");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            for (SeasonType s : SeasonType.values()) {
                completions.add(s.name().toLowerCase());
            }
        }
        return completions;
    }
}

package com.smp.smptools.events.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.EventManager;
import com.smp.smptools.events.gui.EventGUI;
import com.smp.smptools.events.minievents.MiniEventType;
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
import java.util.stream.Collectors;

/**
 * Command handler for /event.
 */
public class EventCommand implements CommandExecutor, TabCompleter {

    private final SMPTools plugin;
    private final EventManager eventManager;
    private final EventGUI eventGUI;

    public EventCommand(SMPTools plugin, EventManager eventManager, EventGUI eventGUI) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.eventGUI = eventGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                eventGUI.openGUI(player);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can open the Event GUI.</red>"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("start")) {
            if (!sender.hasPermission("smptools.events.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to start events.</red>"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /event start <type> [duration_minutes]</red>"));
                return true;
            }

            MiniEventType type;
            try {
                type = MiniEventType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid event type. Available: " + Arrays.toString(MiniEventType.values()) + "</red>"));
                return true;
            }

            int duration = 15;
            if (args.length >= 3) {
                try {
                    duration = Integer.parseInt(args[2]);
                } catch (NumberFormatException ignored) {}
            }

            boolean started = eventManager.startEvent(type, duration);
            if (started) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Successfully started event: " + type.getFormattedName() + "</green>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>An event is already active! Stop it first with /event stop.</red>"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("stop")) {
            if (!sender.hasPermission("smptools.events.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to stop events.</red>"));
                return true;
            }

            boolean stopped = eventManager.stopActiveEvent();
            if (stopped) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Stopped active event.</green>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>No event is currently active.</red>"));
            }
            return true;
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand. Use /event, /event start <type>, or /event stop.</red>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("start");
            completions.add("stop");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            for (MiniEventType t : MiniEventType.values()) {
                completions.add(t.name().toLowerCase());
            }
        }
        return completions;
    }
}

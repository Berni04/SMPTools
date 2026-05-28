package com.smp.smptools.commands;

import com.smp.smptools.managers.BlackFridayManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BlackFridayCommand implements CommandExecutor {

    private final BlackFridayManager manager;

    public BlackFridayCommand(BlackFridayManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
            case "enable":
                manager.setEnabled(true);
                manager.broadcastToggle(true);
                sender.sendMessage(Component.text("Black Friday event has been enabled!", NamedTextColor.GREEN));
                break;

            case "stop":
            case "disable":
                manager.setEnabled(false);
                manager.broadcastToggle(false);
                sender.sendMessage(Component.text("Black Friday event has been disabled!", NamedTextColor.RED));
                break;

            case "reload":
                manager.reloadConfig();
                sender.sendMessage(Component.text("Black Friday configuration reloaded!", NamedTextColor.GREEN));
                break;

            case "status":
                boolean enabled = manager.isEnabled();
                int discount = manager.getDiscountPercentage();
                sender.sendMessage(Component.text("=== Black Friday Status ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Enabled: ", NamedTextColor.YELLOW)
                        .append(Component.text(enabled ? "YES" : "NO", enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
                sender.sendMessage(Component.text("Discount: ", NamedTextColor.YELLOW)
                        .append(Component.text(discount + "%", NamedTextColor.AQUA)));
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Black Friday Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/blackfriday start", NamedTextColor.YELLOW)
                .append(Component.text(" - Enable the event", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/blackfriday stop", NamedTextColor.YELLOW)
                .append(Component.text(" - Disable the event", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/blackfriday reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload configuration", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/blackfriday status", NamedTextColor.YELLOW)
                .append(Component.text(" - View event status", NamedTextColor.GRAY)));
    }
}

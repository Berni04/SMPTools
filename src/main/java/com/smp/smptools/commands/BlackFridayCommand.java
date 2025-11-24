package com.smp.smptools.commands;

import com.smp.smptools.managers.BlackFridayManager;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
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
                sender.sendMessage(ChatColor.GREEN + "Black Friday event has been enabled!");
                break;

            case "stop":
            case "disable":
                manager.setEnabled(false);
                manager.broadcastToggle(false);
                sender.sendMessage(ChatColor.RED + "Black Friday event has been disabled!");
                break;

            case "reload":
                manager.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Black Friday configuration reloaded!");
                break;

            case "status":
                boolean enabled = manager.isEnabled();
                int discount = manager.getDiscountPercentage();
                sender.sendMessage(ChatColor.GOLD + "=== Black Friday Status ===");
                sender.sendMessage(
                        ChatColor.YELLOW + "Enabled: " + (enabled ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));
                sender.sendMessage(ChatColor.YELLOW + "Discount: " + ChatColor.AQUA + discount + "%");
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Black Friday Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/blackfriday start" + ChatColor.GRAY + " - Enable the event");
        sender.sendMessage(ChatColor.YELLOW + "/blackfriday stop" + ChatColor.GRAY + " - Disable the event");
        sender.sendMessage(ChatColor.YELLOW + "/blackfriday reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/blackfriday status" + ChatColor.GRAY + " - View event status");
    }
}

package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ClearStatsCommand implements CommandExecutor {

    private final SMPTools plugin;

    public ClearStatsCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smptools.clearstats")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /clearstats <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        plugin.getStatsConfig().set("stats." + target.getUniqueId().toString(), null);
        plugin.saveStatsConfig();

        sender.sendMessage(ChatColor.GREEN + "Stats for " + target.getName() + " have been cleared.");
        return true;
    }
}

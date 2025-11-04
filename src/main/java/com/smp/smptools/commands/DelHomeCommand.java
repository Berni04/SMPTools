package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DelHomeCommand implements CommandExecutor {

    private final SMPTools plugin;

    public DelHomeCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Please specify the name of the home you want to delete. Usage: /delhome <name>");
            return true;
        }

        Player player = (Player) sender;
        String playerUUID = player.getUniqueId().toString();
        String homeName = args[0].toLowerCase();

        if (plugin.getConfig().contains("homes." + playerUUID + "." + homeName)) {
            plugin.getConfig().set("homes." + playerUUID + "." + homeName, null);
            plugin.saveConfig();
            player.sendMessage(ChatColor.GREEN + "Your home '" + homeName + "' has been deleted!");
        } else {
            player.sendMessage(ChatColor.RED + "You don't have a home named '" + homeName + "'.");
        }
        return true;
    }
}

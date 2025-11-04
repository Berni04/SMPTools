package com.smp.smptools.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (player.hasPermission("smptools.fly")) {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(ChatColor.YELLOW + "Flight disabled.");
            } else {
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage(ChatColor.YELLOW + "Flight enabled.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
        }

        return true;
    }
}

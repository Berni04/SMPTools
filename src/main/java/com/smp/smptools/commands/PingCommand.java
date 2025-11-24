package com.smp.smptools.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player target = (Player) sender;

        if (args.length > 0) {
            Player foundPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
            if (foundPlayer == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }
            target = foundPlayer;
        }

        int ping = target.getPing();

        String color;
        if (ping < 50) {
            color = ChatColor.GREEN.toString();
        } else if (ping < 100) {
            color = ChatColor.YELLOW.toString();
        } else {
            color = ChatColor.RED.toString();
        }

        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.GRAY + "Your ping is: " + color + ping + "ms");
        } else {
            sender.sendMessage(ChatColor.GRAY + target.getName() + "'s ping is: " + color + ping + "ms");
        }

        return true;
    }
}

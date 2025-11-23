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

        Player player = (Player) sender;
        int ping = player.getPing();

        String color;
        if (ping < 50) {
            color = ChatColor.GREEN.toString();
        } else if (ping < 100) {
            color = ChatColor.YELLOW.toString();
        } else {
            color = ChatColor.RED.toString();
        }

        player.sendMessage(ChatColor.GRAY + "Your ping is: " + color + ping + "ms");

        return true;
    }
}

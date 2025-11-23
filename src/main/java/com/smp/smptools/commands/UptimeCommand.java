package com.smp.smptools.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public class UptimeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();

        long days = TimeUnit.MILLISECONDS.toDays(uptimeMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis) % 60;

        StringBuilder uptimeMessage = new StringBuilder(ChatColor.GREEN + "Server Uptime: " + ChatColor.WHITE);
        if (days > 0)
            uptimeMessage.append(days).append("d ");
        if (hours > 0)
            uptimeMessage.append(hours).append("h ");
        if (minutes > 0)
            uptimeMessage.append(minutes).append("m ");
        uptimeMessage.append(seconds).append("s");

        sender.sendMessage(uptimeMessage.toString());

        return true;
    }
}

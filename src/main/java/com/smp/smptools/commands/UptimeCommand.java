package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

        StringBuilder uptimeString = new StringBuilder();
        if (days > 0)
            uptimeString.append(days).append("d ");
        if (hours > 0)
            uptimeString.append(hours).append("h ");
        if (minutes > 0)
            uptimeString.append(minutes).append("m ");
        uptimeString.append(seconds).append("s");

        sender.sendMessage(Component.text("Server Uptime: ", NamedTextColor.GREEN)
                .append(Component.text(uptimeString.toString(), NamedTextColor.WHITE)));

        return true;
    }
}

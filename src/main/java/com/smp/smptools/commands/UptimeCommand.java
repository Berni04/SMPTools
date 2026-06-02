package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UptimeCommand extends AbstractPlayerCommand {

    public UptimeCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
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

        player.sendMessage(plugin.getMessageManager().getMessage("uptime.server-uptime", player, Map.of("uptime", uptimeString.toString())));

        return true;
    }
}

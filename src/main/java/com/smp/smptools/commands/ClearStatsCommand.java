package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /clearstats <player>", NamedTextColor.RED));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        plugin.getStatsConfig().set("stats." + target.getUniqueId().toString(), null);
        plugin.saveStatsConfig();

        sender.sendMessage(Component.text("Stats for " + target.getName() + " have been cleared.", NamedTextColor.GREEN));
        return true;
    }
}

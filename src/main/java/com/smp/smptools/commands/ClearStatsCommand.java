package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearStatsCommand extends AbstractPlayerCommand {

    public ClearStatsCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean allowConsole() {
        return true;
    }

    private boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("smptools.clearstats")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.usage",
                    sender instanceof Player p ? p : null,
                    java.util.Map.of("usage", "/clearstats <player>")));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return true;
        }

        String activeTrail = plugin.getStatsConfig().getString("stats." + target.getUniqueId().toString() + ".active_trail");
        plugin.getStatsConfig().set("stats." + target.getUniqueId().toString(), null);
        if (activeTrail != null) {
            plugin.getStatsConfig().set("stats." + target.getUniqueId().toString() + ".active_trail", activeTrail);
        }
        plugin.saveStatsConfig();
        if (plugin.getStorageManager() != null && plugin.getStorageManager().getProvider() != null) {
            plugin.getStorageManager().getProvider().clearPlayerStats(target.getUniqueId());
        }
        if (plugin.getTagManager() != null) {
            plugin.getTagManager().clearCachedStats(target.getUniqueId());
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("clearstats.cleared",
                sender instanceof Player p ? p : null,
                java.util.Map.of("target", args[0])));
        return true;
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        return execute(player, args);
    }

    @Override
    protected boolean onConsoleCommand(CommandSender sender, Command command, String label, String[] args) {
        return execute(sender, args);
    }
}

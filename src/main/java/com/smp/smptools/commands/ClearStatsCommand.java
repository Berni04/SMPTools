package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class ClearStatsCommand extends AbstractPlayerCommand {

    public ClearStatsCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.clearstats")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/clearstats <player>")));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return true;
        }

        plugin.getStatsConfig().set("stats." + target.getUniqueId().toString(), null);
        plugin.saveStatsConfig();

        player.sendMessage(plugin.getMessageManager().getMessage("clearstats.cleared", player,
                java.util.Map.of("target", args[0])));
        return true;
    }
}

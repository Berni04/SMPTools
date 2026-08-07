package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TradeCommand implements CommandExecutor {

    private final SMPTools plugin;

    public TradeCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.player-only"));
            return true;
        }

        if (!plugin.getConfig().getBoolean("features.remote-trade.enabled", true)) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.error", player, java.util.Map.of("message", "Remote trade is disabled.")));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player, java.util.Map.of("usage", "/trade <player|accept|deny|cancel>")));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "accept":
                plugin.getTradeManager().acceptRequest(player);
                break;
            case "deny":
                plugin.getTradeManager().denyRequest(player);
                break;
            case "cancel":
                if (plugin.getTradeManager().isTrading(player)) {
                    var session = plugin.getTradeManager().getSession(player);
                    if (session != null) {
                        session.cancelTrade(player);
                    }
                } else {
                    player.sendMessage(plugin.getMessageManager().getMessage("trade.no-request"));
                }
                break;
            default:
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
                    return true;
                }
                plugin.getTradeManager().sendRequest(player, target);
                break;
        }

        return true;
    }
}

package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class PingCommand extends AbstractPlayerCommand {

    public PingCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean allowConsole() {
        return true;
    }

    private boolean execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            Player foundPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
            if (foundPlayer == null) {
                sender.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
                return true;
            }
            showPing(sender, foundPlayer);
        } else {
            if (sender instanceof Player player) {
                showPing(sender, player);
            } else {
                sender.sendMessage(plugin.getMessageManager().getMessage("common.usage",
                        null,
                        Map.of("usage", "/ping <player>")));
            }
        }

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

    private void showPing(CommandSender requester, Player target) {
        int ping = target.getPing();

        Player requesterPlayer = requester instanceof Player p ? p : null;
        if (requesterPlayer != null && target.equals(requesterPlayer)) {
            requester.sendMessage(plugin.getMessageManager().getMessage("ping.your-ping", requesterPlayer,
                    Map.of("ping", String.valueOf(ping))));
        } else {
            requester.sendMessage(plugin.getMessageManager().getMessage("ping.player-ping", requesterPlayer,
                    Map.of("player", target.getName(), "ping", String.valueOf(ping))));
        }
    }
}

package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.Map;

public class PingCommand extends AbstractPlayerCommand {

    public PingCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length > 0) {
            Player foundPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
            if (foundPlayer == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
                return true;
            }
            showPing(player, foundPlayer);
        } else {
            showPing(player, player);
        }

        return true;
    }

    private void showPing(Player requester, Player target) {
        int ping = target.getPing();

        if (target.equals(requester)) {
            requester.sendMessage(plugin.getMessageManager().getMessage("ping.your-ping", requester, Map.of("ping", String.valueOf(ping))));
        } else {
            requester.sendMessage(plugin.getMessageManager().getMessage("ping.player-ping", requester, Map.of("player", target.getName(), "ping", String.valueOf(ping))));
        }
    }
}

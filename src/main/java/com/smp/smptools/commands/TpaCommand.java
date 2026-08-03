package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class TpaCommand extends AbstractPlayerCommand {

    public TpaCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("tpr")) {
            if (args.length == 0) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                        java.util.Map.of("usage", "/tpr <player>")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
                return true;
            }
            if (player.equals(target)) {
                player.sendMessage(plugin.getMessageManager().getMessage("tpa.self-request"));
                return true;
            }
            plugin.getTpaManager().sendTeleportRequest(player, target);
            return true;
        } else if (label.equalsIgnoreCase("tpa")) {
            plugin.getTpaManager().acceptTeleportRequest(player);
            return true;
        } else if (label.equalsIgnoreCase("tpd")) {
            plugin.getTpaManager().denyTeleportRequest(player);
            return true;
        } else if (label.equalsIgnoreCase("tptoggle")) {
            plugin.getTpaManager().toggleTpa(player);
            return true;
        }

        return false;
    }
}

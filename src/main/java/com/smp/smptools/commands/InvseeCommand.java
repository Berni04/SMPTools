package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.listeners.InvseeGUIListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class InvseeCommand extends AbstractPlayerCommand {

    public InvseeCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.invsee")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/invsee <player>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getMessageManager().getMessage("invsee.self-use"));
            return true;
        }

        player.sendMessage(plugin.getMessageManager().getMessage("invsee.viewing", player,
                java.util.Map.of("target", target.getName())));
        plugin.getInvseeGUIListener().openInvseeGUI(player, target);

        return true;
    }
}

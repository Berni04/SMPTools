package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class FlyCommand extends AbstractPlayerCommand {

    public FlyCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.fly")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.sendMessage(plugin.getMessageManager().getMessage("fly.disabled"));
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.sendMessage(plugin.getMessageManager().getMessage("fly.enabled"));
        }

        return true;
    }
}

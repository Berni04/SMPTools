package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.KrampusManager;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class KrampusCommand extends AbstractPlayerCommand {

    private final KrampusManager krampusManager;

    public KrampusCommand(SMPTools plugin, KrampusManager krampusManager) {
        super(plugin);
        this.krampusManager = krampusManager;
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("spawn")) {
            krampusManager.spawnKrampus(player.getLocation());
            player.sendMessage(plugin.getMessageManager().getMessage("krampus.spawned"));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/krampus spawn")));
        }

        return true;
    }
}

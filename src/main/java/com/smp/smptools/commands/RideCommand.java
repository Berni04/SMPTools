package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.InputValidator;
import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RideCommand extends AbstractPlayerCommand {

    public RideCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
            player.sendMessage(plugin.getMessageManager().getMessage("ride.dismounted"));
            return true;
        }

        List<Entity> nearbyEntities = player.getNearbyEntities(5, 5, 5);
        List<Entity> targetEntities = nearbyEntities.stream()
                .filter(player::hasLineOfSight)
                .filter(entity -> !entity.equals(player))
                .collect(Collectors.toList());

        if (targetEntities.isEmpty()) {
            player.sendMessage(plugin.getMessageManager().getMessage("ride.no-entity"));
            return true;
        }

        Entity target = targetEntities.get(0);
        target.addPassenger(player);
        player.sendMessage(plugin.getMessageManager().getMessage("ride.riding", player,
                Map.of("target", InputValidator.sanitizeMiniMessage(target.getName()))));

        return true;
    }
}

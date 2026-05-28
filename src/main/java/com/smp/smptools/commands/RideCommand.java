package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class RideCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        // If player is already riding something, dismount them.
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
            player.sendMessage(Component.text("You have dismounted.", NamedTextColor.YELLOW));
            return true;
        }

        // Find the entity the player is looking at
        List<Entity> nearbyEntities = player.getNearbyEntities(5, 5, 5);
        List<Entity> targetEntities = nearbyEntities.stream()
                .filter(player::hasLineOfSight)
                .filter(entity -> !entity.equals(player))
                .collect(Collectors.toList());

        if (targetEntities.isEmpty()) {
            player.sendMessage(Component.text("You are not looking at a nearby entity.", NamedTextColor.RED));
            return true;
        }

        Entity target = targetEntities.get(0);

        // Add the player as a passenger
        target.addPassenger(player);
        player.sendMessage(Component.text("You are now riding " + target.getName() + "!", NamedTextColor.GREEN));

        return true;
    }
}

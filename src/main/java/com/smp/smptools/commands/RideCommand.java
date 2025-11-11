package com.smp.smptools.commands;

import org.bukkit.ChatColor;
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
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // If player is already riding something, dismount them.
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
            player.sendMessage(ChatColor.YELLOW + "You have dismounted.");
            return true;
        }

        // Find the entity the player is looking at
        List<Entity> nearbyEntities = player.getNearbyEntities(5, 5, 5);
        List<Entity> targetEntities = nearbyEntities.stream()
                .filter(player::hasLineOfSight)
                .filter(entity -> !entity.equals(player))
                .collect(Collectors.toList());

        if (targetEntities.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are not looking at a nearby entity.");
            return true;
        }

        Entity target = targetEntities.get(0);

        // Add the player as a passenger
        target.addPassenger(player);
        player.sendMessage(ChatColor.GREEN + "You are now riding " + target.getName() + "!");

        return true;
    }
}

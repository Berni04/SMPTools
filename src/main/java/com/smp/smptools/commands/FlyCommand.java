package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (player.hasPermission("smptools.fly")) {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(Component.text("Flight disabled.", NamedTextColor.YELLOW));
            } else {
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage(Component.text("Flight enabled.", NamedTextColor.YELLOW));
            }
        } else {
            player.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
        }

        return true;
    }
}

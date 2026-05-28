package com.smp.smptools.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PingCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Player target = (Player) sender;

        if (args.length > 0) {
            Player foundPlayer = org.bukkit.Bukkit.getPlayer(args[0]);
            if (foundPlayer == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return true;
            }
            target = foundPlayer;
        }

        int ping = target.getPing();

        NamedTextColor color;
        if (ping < 50) {
            color = NamedTextColor.GREEN;
        } else if (ping < 100) {
            color = NamedTextColor.YELLOW;
        } else {
            color = NamedTextColor.RED;
        }

        if (target.equals(sender)) {
            sender.sendMessage(Component.text("Your ping is: ", NamedTextColor.GRAY)
                    .append(Component.text(ping + "ms", color)));
        } else {
            sender.sendMessage(Component.text(target.getName() + "'s ping is: ", NamedTextColor.GRAY)
                    .append(Component.text(ping + "ms", color)));
        }

        return true;
    }
}

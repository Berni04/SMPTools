package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SudoCommand implements CommandExecutor {

    private final SMPTools plugin;

    public SudoCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smptools.sudo")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /sudo <player> <command or chat message>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            sender.sendMessage(Component.text("Player '" + args[0] + "' not found.", NamedTextColor.RED));
            return true;
        }

        String commandToExecute = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        // If it starts with '/', it's a command. Otherwise, it's a chat message.
        if (commandToExecute.startsWith("/")) {
            target.performCommand(commandToExecute.substring(1));
        } else {
            target.chat(commandToExecute);
        }

        sender.sendMessage(Component.text("Forced " + target.getName() + " to execute: ", NamedTextColor.GREEN)
                .append(Component.text(commandToExecute, NamedTextColor.WHITE)));

        return true;
    }
}

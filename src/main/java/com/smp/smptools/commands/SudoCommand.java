package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.CommandBlacklist;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class SudoCommand implements CommandExecutor {

    private final SMPTools plugin;

    public SudoCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smptools.sudo")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.usage",
                    sender instanceof Player ? (Player) sender : null,
                    java.util.Map.of("usage", "/sudo <player> [--confirm] <command or chat message>")));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return true;
        }

        int commandStartIndex = 1;
        boolean confirmed = false;
        if (args.length > 2 && args[1].equalsIgnoreCase("--confirm")) {
            confirmed = true;
            commandStartIndex = 2;
        }

        // Target protection: If target is OP or has smptools.sudo, require --confirm flag
        if ((target.isOp() || target.hasPermission("smptools.sudo")) && !confirmed) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target player is an Operator or Administrator. Add '--confirm' flag to force command execution.</red>"));
            return true;
        }

        String commandToExecute = String.join(" ", Arrays.copyOfRange(args, commandStartIndex, args.length));

        if (commandToExecute.startsWith("/")) {
            String stripped = commandToExecute.substring(1);
            if (CommandBlacklist.isBlocked(stripped)) {
                sender.sendMessage(plugin.getMessageManager().getMessage("sudo.blocked",
                        sender instanceof Player ? (Player) sender : null));
                return true;
            }
            target.performCommand(stripped);
        } else {
            target.chat(commandToExecute);
        }

        sender.sendMessage(plugin.getMessageManager().getMessage("sudo.forced",
                sender instanceof Player ? (Player) sender : null,
                java.util.Map.of("target", target.getName(), "command", commandToExecute)));

        return true;
    }
}

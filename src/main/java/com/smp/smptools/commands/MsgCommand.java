package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MsgCommand implements CommandExecutor {

    private final SMPTools plugin;

    public MsgCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /msg <player> <message>");
            return true;
        }

        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        Player senderPlayer = (Player) sender;
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        String senderDisplayName = senderPlayer.getDisplayName();
        String recipientDisplayName = recipient.getDisplayName();

        senderPlayer.sendMessage("You -> " + recipientDisplayName + ": " + message);
        recipient.sendMessage(senderDisplayName + " -> You: " + message);
        recipient.playSound(recipient.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

        return true;
    }
}

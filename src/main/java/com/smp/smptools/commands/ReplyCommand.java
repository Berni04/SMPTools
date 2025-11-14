package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    private final SMPTools plugin;

    public ReplyCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command!</red>"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /r <message></red>"));
            return true;
        }

        UUID lastMessengerUUID = plugin.getChatManager().getLastMessenger(player.getUniqueId());

        if (lastMessengerUUID == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have nobody to reply to!</red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(lastMessengerUUID);

        if (target == null || !target.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Your last messenger is no longer online.</red>"));
            // Clear the last messenger if they are offline
            plugin.getChatManager().setLastMessenger(player.getUniqueId(), null);
            return true;
        }

        // Construct the message
        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String messageContent = messageBuilder.toString().trim();

        // Get formatted display names
        Component formattedSenderName = plugin.getChatManager().getFormattedDisplayName(player);
        Component formattedRecipientName = plugin.getChatManager().getFormattedDisplayName(target);

        // Message for the recipient (from sender)
        Component recipientMessage = Component.text("(from ", NamedTextColor.GRAY)
                .append(formattedSenderName)
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(messageContent, NamedTextColor.WHITE));
        
        // Message for the sender (to recipient)
        Component senderMessage = Component.text("(to ", NamedTextColor.GRAY)
                .append(formattedRecipientName)
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(messageContent, NamedTextColor.WHITE));

        target.sendMessage(recipientMessage);
        player.sendMessage(senderMessage);

        // Update last messengers for both players
        plugin.getChatManager().setLastMessenger(player.getUniqueId(), target.getUniqueId());
        plugin.getChatManager().setLastMessenger(target.getUniqueId(), player.getUniqueId());

        return true;
    }
}

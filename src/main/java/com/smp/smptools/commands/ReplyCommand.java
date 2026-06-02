package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReplyCommand extends AbstractPlayerCommand {

    public ReplyCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/r <message>")));
            return true;
        }

        UUID lastMessengerUUID = plugin.getChatManager().getLastMessenger(player.getUniqueId());

        if (lastMessengerUUID == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("chat.no-reply"));
            return true;
        }

        Player target = Bukkit.getPlayer(lastMessengerUUID);

        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getMessageManager().getMessage("chat.reply-offline"));
            plugin.getChatManager().setLastMessenger(player.getUniqueId(), null);
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String messageContent = messageBuilder.toString().trim();

        Component formattedSenderName = plugin.getChatManager().getFormattedDisplayName(player);
        Component formattedRecipientName = plugin.getChatManager().getFormattedDisplayName(target);

        Component recipientMessage = Component.text("(from ", NamedTextColor.GRAY)
                .append(formattedSenderName)
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(messageContent, NamedTextColor.WHITE));

        Component senderMessage = Component.text("(to ", NamedTextColor.GRAY)
                .append(formattedRecipientName)
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(messageContent, NamedTextColor.WHITE));

        target.sendMessage(recipientMessage);
        player.sendMessage(senderMessage);

        plugin.getChatManager().setLastMessenger(player.getUniqueId(), target.getUniqueId());
        plugin.getChatManager().setLastMessenger(target.getUniqueId(), player.getUniqueId());

        return true;
    }
}

package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class MsgCommand extends AbstractPlayerCommand {

    public MsgCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player senderPlayer, Command command, String label, String[] args) {
        if (args.length < 2) {
            senderPlayer.sendMessage(plugin.getMessageManager().getMessage("common.usage", senderPlayer,
                    java.util.Map.of("usage", "/msg <player> <message>")));
            return true;
        }

        Player recipient = Bukkit.getPlayer(args[0]);
        if (recipient == null) {
            senderPlayer.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String messageContent = messageBuilder.toString().trim();

        Component formattedSenderName = plugin.getChatManager().getFormattedDisplayName(senderPlayer);
        Component formattedRecipientName = plugin.getChatManager().getFormattedDisplayName(recipient);

        Component recipientMessage = Component.text("(from ", NamedTextColor.GRAY)
                .append(formattedSenderName)
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(messageContent, NamedTextColor.WHITE));

        Component senderMessage = Component.text("(to ", NamedTextColor.GRAY)
                .append(formattedRecipientName)
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(messageContent, NamedTextColor.WHITE));

        senderPlayer.sendMessage(senderMessage);
        recipient.sendMessage(recipientMessage);
        recipient.playSound(recipient.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.0f);

        plugin.getChatManager().setLastMessenger(senderPlayer.getUniqueId(), recipient.getUniqueId());
        plugin.getChatManager().setLastMessenger(recipient.getUniqueId(), senderPlayer.getUniqueId());

        return true;
    }
}

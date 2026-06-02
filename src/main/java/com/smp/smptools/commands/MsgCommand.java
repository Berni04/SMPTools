package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class MsgCommand extends AbstractPlayerCommand {

    /**
     * MiniMessage instance with no registered tags. Used to render usage
     * text containing literal {@code <player>} / {@code <message>} placeholders
     * without those being parsed (and stripped) as unknown tags.
     */
    private static final MiniMessage LITERAL = MiniMessage.builder()
            .tags(TagResolver.empty())
            .build();

    public MsgCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player senderPlayer, Command command, String label, String[] args) {
        if (args.length < 2) {
            // Build the usage Component directly so the < and > around
            // <player> and <message> survive without being interpreted as
            // MiniMessage tags.
            senderPlayer.sendMessage(LITERAL.deserialize("/msg <player> <message>")
                    .color(NamedTextColor.RED));
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

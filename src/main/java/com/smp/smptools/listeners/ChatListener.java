package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatListener implements Listener {

    private final SMPTools plugin;

    public ChatListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Component rawMessage = event.message();

        Component displayName = plugin.getChatManager().getFormattedDisplayName(player);
        Component messageContent = rawMessage.color(NamedTextColor.WHITE);

        if (TrollGUIListener.isChatScrambled(player)) {
            String plainMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(rawMessage);
            List<String> characters = Arrays.asList(plainMessage.split(""));
            Collections.shuffle(characters);
            String scrambled = String.join("", characters);
            messageContent = Component.text(scrambled, NamedTextColor.WHITE);
        }

        final Component formatted = displayName.append(Component.text(": ")).append(messageContent);
        event.renderer((source, sourceDisplayName, message, viewer) -> formatted);
    }
}

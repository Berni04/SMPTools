package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.List;

public class ChatListener implements Listener {

    private final SMPTools plugin;

    public ChatListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();

        Component displayName = plugin.getChatManager().getFormattedDisplayName(player);
        Component messageContent = event.message().color(NamedTextColor.WHITE);

        if (TrollGUIListener.isChatScrambled(player)) {
            String plainMessage = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(event.message());
            List<String> characters = java.util.Arrays.asList(plainMessage.split(""));
            java.util.Collections.shuffle(characters);
            String scrambled = String.join("", characters);
            messageContent = Component.text(scrambled, NamedTextColor.WHITE);
        }

        Component message = Component.text(": ").append(messageContent);

        plugin.getServer().broadcast(displayName.append(message));
    }
}

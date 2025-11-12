package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
        Component message = Component.text(": ").append(event.message().color(NamedTextColor.WHITE));

        plugin.getServer().broadcast(displayName.append(message));
    }
}

package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        SMPTools plugin = SMPTools.getInstance();

        String prefix = plugin.getStatsConfig().getString("players." + player.getUniqueId() + ".prefix");
        String nameColor = plugin.getStatsConfig().getString("players." + player.getUniqueId() + ".name-color");

        Component prefixComponent = Component.empty();
        if (prefix != null && !prefix.isEmpty()) {
            String coloredPrefix = (nameColor != null ? nameColor : "") + prefix;
            prefixComponent = MiniMessage.miniMessage().deserialize(coloredPrefix).append(Component.space());
        }

        Component nameComponent = MiniMessage.miniMessage().deserialize(player.getName());
        if (nameColor != null && !nameColor.isEmpty()) {
            nameComponent = MiniMessage.miniMessage().deserialize(nameColor + player.getName());
        }

        Component messageComponent = event.message().color(NamedTextColor.WHITE);

        Component finalMessage = prefixComponent.append(nameComponent);

        String playerTitle = plugin.getTagManager().getPlayerTitle(player);
        if (playerTitle != null && !playerTitle.isEmpty()) {
            Component titleComponent = MiniMessage.miniMessage().deserialize((nameColor != null ? nameColor : "") + "[" + playerTitle + "]");

            // Find the description for the title
            String description = plugin.getTagManager().getTagDescription(playerTitle);
            if (description != null) {
                titleComponent = titleComponent.hoverEvent(HoverEvent.showText(Component.text(description)));
            }

            finalMessage = finalMessage.append(Component.space()).append(titleComponent);
        }
        finalMessage = finalMessage.append(Component.space()).append(messageComponent);

        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            onlinePlayer.sendMessage(finalMessage);
        }
    }
}

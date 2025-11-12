package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {

    private final SMPTools plugin;

    public AdvancementListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        // Only process if the advancement has a display component (i.e., it's not a hidden internal advancement)
        if (event.getAdvancement().getDisplay() == null) {
            return;
        }

        // Cancel the default message
        event.message(null);

        Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
        Component advancementTitle = event.getAdvancement().getDisplay().title();
        Component advancementDescription = event.getAdvancement().getDisplay().description();

        // Add hover event to the advancement title
        Component hoverableAdvancementTitle = advancementTitle.color(NamedTextColor.GREEN)
                .hoverEvent(advancementDescription.color(NamedTextColor.GRAY)); // Description also colored

        // Construct the new message: "[PlayerName] has made the advancement [AdvancementTitle]"
        Component finalMessage = formattedPlayerName
                .append(Component.text(" has made the advancement ", NamedTextColor.YELLOW))
                .append(hoverableAdvancementTitle);

        plugin.getServer().broadcast(finalMessage);
    }
}

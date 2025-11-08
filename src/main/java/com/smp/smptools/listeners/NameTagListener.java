package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NameTagListener implements Listener {

    private final SMPTools plugin;

    public NameTagListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerName(event.getPlayer());
    }

    public void updatePlayerName(Player player) {
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

        Component finalName = prefixComponent.append(nameComponent);

        player.displayName(finalName);
        player.playerListName(finalName);
    }
}

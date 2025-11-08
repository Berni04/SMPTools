package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    private final SMPTools plugin;

    public JoinLeaveListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Component joinMessage = getFormattedName(player).append(Component.text(" joined", NamedTextColor.WHITE));
        event.joinMessage(joinMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Component quitMessage = getFormattedName(player).append(Component.text(" left", NamedTextColor.WHITE));
        event.quitMessage(quitMessage);
    }

    private Component getFormattedName(Player player) {
        String prefix = plugin.getStatsConfig().getString("players." + player.getUniqueId() + ".prefix");
        String nameColor = plugin.getStatsConfig().getString("players." + player.getUniqueId() + ".name-color");
        String playerTitle = plugin.getTagManager().getPlayerTitle(player);

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

        if (playerTitle != null && !playerTitle.isEmpty()) {
            Component titleComponent = MiniMessage.miniMessage().deserialize((nameColor != null ? nameColor : "") + "<" + playerTitle + ">");
            finalName = finalName.append(Component.space()).append(titleComponent);
        }

        return finalName;
    }
}

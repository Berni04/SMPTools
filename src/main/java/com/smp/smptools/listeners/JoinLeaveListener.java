package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        Component displayName = plugin.getChatManager().getFormattedDisplayName(player);
        Component joinMessage = displayName.append(Component.text(" has joined the server.", NamedTextColor.YELLOW));
        event.joinMessage(joinMessage);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Component displayName = plugin.getChatManager().getFormattedDisplayName(player);
        Component quitMessage = displayName.append(Component.text(" has left the server.", NamedTextColor.YELLOW));
        event.quitMessage(quitMessage);
    }
}

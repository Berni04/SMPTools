package com.smp.smptools.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        org.bukkit.entity.Player player = event.getPlayer();
        String playerName = player.getDisplayName();
        com.smp.smptools.SMPTools plugin = com.smp.smptools.SMPTools.getInstance();
        String prefix = plugin.getConfig().getString("player-prefix." + player.getUniqueId());
        if (prefix != null) {
            event.setFormat(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix) + " " + playerName + ": " + org.bukkit.ChatColor.WHITE + message);
        } else {
            event.setFormat(playerName + ": " + org.bukkit.ChatColor.WHITE + message);
        }
    }
}

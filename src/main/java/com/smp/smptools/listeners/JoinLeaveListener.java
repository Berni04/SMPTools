package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
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
        String playerUUID = player.getUniqueId().toString();

        String prefix = plugin.getStatsConfig().getString("stats." + playerUUID + ".prefix", "");
        String color = plugin.getStatsConfig().getString("stats." + playerUUID + ".color", "WHITE");

        try {
            ChatColor chatColor = ChatColor.valueOf(color.toUpperCase());
            String joinMessage = chatColor + prefix + " " + player.getName() + ChatColor.WHITE + " joined";
            event.setJoinMessage(joinMessage);
        } catch (IllegalArgumentException e) {
            String joinMessage = ChatColor.WHITE + "[" + prefix + "] " + player.getName() + ChatColor.WHITE + " joined";
            event.setJoinMessage(joinMessage);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        String prefix = plugin.getStatsConfig().getString("stats." + playerUUID + ".prefix", "");
        String color = plugin.getStatsConfig().getString("stats." + playerUUID + ".color", "WHITE");

        try {
            ChatColor chatColor = ChatColor.valueOf(color.toUpperCase());
            String quitMessage = chatColor + prefix + " " + player.getName() + ChatColor.WHITE + " left";
            event.setQuitMessage(quitMessage);
        } catch (IllegalArgumentException e) {
            String quitMessage = ChatColor.WHITE + "[" + prefix + "] " + player.getName() + ChatColor.WHITE + " left";
            event.setQuitMessage(quitMessage);
        }
    }
}

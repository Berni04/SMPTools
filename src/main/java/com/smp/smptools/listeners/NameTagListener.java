package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
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
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        String prefix = plugin.getStatsConfig().getString("stats." + playerUUID + ".prefix", "");
        String color = plugin.getStatsConfig().getString("stats." + playerUUID + ".color", "WHITE");

        try {
            ChatColor chatColor = ChatColor.valueOf(color.toUpperCase());
            String displayName = chatColor + "[" + prefix + "] " + player.getName();
            player.setDisplayName(displayName);
            player.setPlayerListName(displayName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid color in stats.yml for player " + player.getName());
        }
    }
}

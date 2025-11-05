package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabHealthListener implements Listener {

    private final SMPTools plugin;

    public TabHealthListener(SMPTools plugin) {
        this.plugin = plugin;
        startHealthUpdater();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerHealth(event.getPlayer());
    }

    private void startHealthUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerHealth(player);
            }
        }, 0L, 20L); // Update every second
    }

    private void updatePlayerHealth(Player player) {
        double health = player.getHealth();
        String healthDisplay = getHealthDisplay(health);
        player.setPlayerListName(player.getDisplayName() + " " + healthDisplay);
    }

    private String getHealthDisplay(double health) {
        ChatColor healthColor;
        if (health < 6) {
            healthColor = ChatColor.RED;
        } else if (health < 14) {
            healthColor = ChatColor.YELLOW;
        } else {
            healthColor = ChatColor.GREEN;
        }

        int hearts = (int) Math.round(health / 2.0);
        StringBuilder heartString = new StringBuilder();
        for (int i = 0; i < hearts; i++) {
            heartString.append("❤");
        }

        return healthColor + heartString.toString();
    }
}

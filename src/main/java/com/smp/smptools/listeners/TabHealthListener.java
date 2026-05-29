package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
        }, 0L, 20L);
    }

    private void updatePlayerHealth(Player player) {
        double health = player.getHealth();
        Component healthDisplay = getHealthDisplay(health);
        player.playerListName(player.displayName().append(Component.text(" ")).append(healthDisplay));
    }

    private Component getHealthDisplay(double health) {
        NamedTextColor healthColor;
        if (health < 6) {
            healthColor = NamedTextColor.RED;
        } else if (health < 14) {
            healthColor = NamedTextColor.YELLOW;
        } else {
            healthColor = NamedTextColor.GREEN;
        }

        int hearts = (int) Math.round(health / 2.0);
        StringBuilder heartString = new StringBuilder();
        for (int i = 0; i < hearts; i++) {
            heartString.append("❤");
        }

        return Component.text(heartString.toString(), healthColor);
    }
}

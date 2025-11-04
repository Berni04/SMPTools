package com.smp.smptools.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class SleepListener implements Listener {

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return; // Player couldn't enter the bed for some reason
        }

        Player player = event.getPlayer();
        World world = player.getWorld();

        // Check if it's night time
        if (world.getTime() >= 12541 && world.getTime() <= 23458) { // Night time in Minecraft
            int sleepingPlayers = 0;
            for (Player p : world.getPlayers()) {
                if (p.isSleeping()) {
                    sleepingPlayers++;
                }
            }

            // If only one player is sleeping, skip the night
            if (sleepingPlayers == 1) {
                world.setTime(0); // Set time to dawn
                world.setThundering(false);
                world.setStorm(false);
                Bukkit.broadcastMessage(ChatColor.GOLD + "One player slept, skipping the night!");
            }
        }
    }
}

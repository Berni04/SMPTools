package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.sleep.SleepManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SleepListener implements Listener {

    private final SMPTools plugin;
    private final SleepManager sleepManager;

    public SleepListener(SMPTools plugin) {
        this.plugin = plugin;
        this.sleepManager = plugin.getSleepManager();
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (!plugin.getConfig().getBoolean("features.sleep-voting.enabled", true)) {
            return;
        }

        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getWorld().getTime() < 12541) {
            return; // It's not night
        }

        // Prevent starting a vote if one is already in progress by another player
        if (sleepManager.isVoteInProgress()) {
             player.sendMessage("A sleep vote is already in progress.");
            return;
        }

        // Use a short delay to ensure the player is fully "in" the bed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isSleeping()) {
                sleepManager.startVote(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (sleepManager.isVoteInProgress() && event.getPlayer().equals(sleepManager.getVoteInitiator())) {
            sleepManager.endVote();
            plugin.getServer().broadcastMessage("The sleep vote was cancelled because the initiator left.");
        }
    }
}

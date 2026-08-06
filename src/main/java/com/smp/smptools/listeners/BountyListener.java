package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.bounty.BountyManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.event.player.PlayerJoinEvent;

public class BountyListener implements Listener {

    private final SMPTools plugin;
    private final BountyManager bountyManager;

    public BountyListener(SMPTools plugin) {
        this.plugin = plugin;
        this.bountyManager = plugin.getBountyManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bountyManager != null) {
            bountyManager.checkPlayerJoin(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) return;

        if (bountyManager.onPlayerKilled(victim, killer)) {
            killer.sendMessage(MiniMessage.miniMessage().deserialize(
                    "<gold>⚔ You defeated <red>" + victim.getName() + "</red>! You have won their bounty! Type <yellow>/bounty claim</yellow> to claim your items! (Expires in 7 days)</gold>"
            ));

            Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                    "<gold>⚔ " + killer.getName() + "</gold> <gray>defeated</gray> <red>" + victim.getName() + "</red> <gray>and can claim their bounty using /bounty claim!</gray>"
            ));
        }
    }
}

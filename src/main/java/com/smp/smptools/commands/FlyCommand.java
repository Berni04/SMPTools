package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlyCommand extends AbstractPlayerCommand implements Listener {

    private final Set<UUID> pluginFlyingPlayers = ConcurrentHashMap.newKeySet();

    public FlyCommand(SMPTools plugin) {
        super(plugin);
        if (plugin != null && Bukkit.getServer() != null) {
            try {
                Bukkit.getPluginManager().registerEvents(this, plugin);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.fly")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            pluginFlyingPlayers.remove(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("fly.disabled"));
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            pluginFlyingPlayers.add(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getMessage("fly.enabled"));
        }

        return true;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (pluginFlyingPlayers.remove(player.getUniqueId())) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (pluginFlyingPlayers.remove(player.getUniqueId())) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(plugin.getMessageManager().getMessage("fly.disabled"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (pluginFlyingPlayers.remove(player.getUniqueId())) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }
}

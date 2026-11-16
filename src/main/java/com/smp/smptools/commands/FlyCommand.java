package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FlyCommand extends AbstractPlayerCommand implements Listener {

    private final Set<UUID> pluginFlyingPlayers = ConcurrentHashMap.newKeySet();
    private final org.bukkit.NamespacedKey flyKey;

    public FlyCommand(SMPTools plugin) {
        super(plugin);
        this.flyKey = plugin != null ? new org.bukkit.NamespacedKey(plugin, "fly_command_enabled") : null;
        if (plugin != null && Bukkit.getServer() != null) {
            try {
                Bukkit.getPluginManager().registerEvents(this, plugin);
            } catch (Exception ignored) {}
        }
    }

    private boolean isPluginFlying(Player player) {
        if (player == null) return false;
        if (pluginFlyingPlayers.contains(player.getUniqueId())) return true;
        return flyKey != null && player.getPersistentDataContainer().has(flyKey, org.bukkit.persistence.PersistentDataType.BYTE);
    }

    private void removePluginFlying(Player player) {
        if (player == null) return;
        pluginFlyingPlayers.remove(player.getUniqueId());
        if (flyKey != null) {
            player.getPersistentDataContainer().remove(flyKey);
        }
    }

    private void setPluginFlying(Player player) {
        if (player == null) return;
        pluginFlyingPlayers.add(player.getUniqueId());
        if (flyKey != null) {
            player.getPersistentDataContainer().set(flyKey, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
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
            removePluginFlying(player);
            player.sendMessage(plugin.getMessageManager().getMessage("fly.disabled"));
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
            setPluginFlying(player);
            player.sendMessage(plugin.getMessageManager().getMessage("fly.enabled"));
        }

        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (isPluginFlying(player)) {
            removePluginFlying(player);
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (isPluginFlying(player)) {
            removePluginFlying(player);
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
        if (isPluginFlying(player)) {
            removePluginFlying(player);
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isPluginFlying(player)) {
            if (player.hasPermission("smptools.fly")) {
                pluginFlyingPlayers.add(player.getUniqueId());
                player.setAllowFlight(true);
            } else {
                removePluginFlying(player);
                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }
        }
    }
}

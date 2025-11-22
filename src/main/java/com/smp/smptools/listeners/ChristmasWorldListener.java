package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.ChatColor;

public class ChristmasWorldListener implements Listener {

    private final SMPTools plugin;
    private static final String WORLD_NAME = "christmas";

    public ChristmasWorldListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getNewState().getType() == Material.SNOW || event.getNewState().getType() == Material.ICE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                    && event.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                event.getPlayer().setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE
                    && event.getPlayer().getGameMode() != GameMode.SPECTATOR) {
                event.getPlayer().setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            Material type = event.getBlock().getType();
            if (type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE
                    || type == Material.FROSTED_ICE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.getPlayer().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You cannot sleep in this world!");
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD
                    || event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (event.getEntity().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.isGliding()) { // Trying to start gliding
                event.setCancelled(true);
                if (event.getEntity() instanceof Player) {
                    ((Player) event.getEntity())
                            .sendMessage(ChatColor.RED + "Elytra flying is disabled in this world!");
                }
            }
        }
    }
}

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

    public static final org.bukkit.NamespacedKey ORIG_GAMEMODE_KEY = new org.bukkit.NamespacedKey(SMPTools.getInstance(), "orig_gamemode");

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            // Entering christmas world
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.getPersistentDataContainer().set(ORIG_GAMEMODE_KEY, org.bukkit.persistence.PersistentDataType.STRING, player.getGameMode().name());
                player.setGameMode(GameMode.ADVENTURE);
            }
        } else if (event.getFrom().getName().equalsIgnoreCase(WORLD_NAME)) {
            // Leaving christmas world
            restoreOriginalGameMode(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                if (!player.getPersistentDataContainer().has(ORIG_GAMEMODE_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
                    player.getPersistentDataContainer().set(ORIG_GAMEMODE_KEY, org.bukkit.persistence.PersistentDataType.STRING, player.getGameMode().name());
                }
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            restoreOriginalGameMode(player);
        }
    }

    private void restoreOriginalGameMode(Player player) {
        if (player.getPersistentDataContainer().has(ORIG_GAMEMODE_KEY, org.bukkit.persistence.PersistentDataType.STRING)) {
            String saved = player.getPersistentDataContainer().get(ORIG_GAMEMODE_KEY, org.bukkit.persistence.PersistentDataType.STRING);
            player.getPersistentDataContainer().remove(ORIG_GAMEMODE_KEY);
            if (saved != null) {
                try {
                    player.setGameMode(GameMode.valueOf(saved));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getWorld().getName().equalsIgnoreCase(WORLD_NAME)) {
            if (event.getEntity() instanceof Player player) {
                event.setCancelled(true);
                if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                    player.teleport(player.getWorld().getSpawnLocation());
                    player.setFallDistance(0);
                }
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
            event.getPlayer().sendMessage(SMPTools.getInstance().getMessageManager().getMessage("christmas.cannot-sleep", event.getPlayer()));
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
            if (event.isGliding()) {
                event.setCancelled(true);
                if (event.getEntity() instanceof Player) {
                    ((Player) event.getEntity())
                            .sendMessage(SMPTools.getInstance().getMessageManager().getMessage("christmas.elytra-disabled", (Player) event.getEntity()));
                }
            }
        }
    }
}

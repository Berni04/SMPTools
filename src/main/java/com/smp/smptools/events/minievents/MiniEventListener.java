package com.smp.smptools.events.minievents;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.EventManager;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Listens to player interactions and awards points to active mini-event sessions.
 */
public class MiniEventListener implements Listener {

    private final SMPTools plugin;
    private final EventManager eventManager;
    private final Random random = new Random();

    public MiniEventListener(SMPTools plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        MiniEventSession activeSession = eventManager.getActiveSession();
        if (activeSession == null || !activeSession.isActive() || activeSession.getType() != MiniEventType.FISHING_DERBY) {
            return;
        }

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item item) {
            Player player = event.getPlayer();
            ItemStack caughtItem = item.getItemStack();
            Material mat = caughtItem.getType();

            int points = 1;
            String label = "Cod";

            if (mat == Material.SALMON) {
                points = 2;
                label = "Salmon";
            } else if (mat == Material.TROPICAL_FISH) {
                points = 3;
                label = "Tropical Fish";
            } else if (mat == Material.PUFFERFISH) {
                points = 5;
                label = "Pufferfish";
            } else if (mat == Material.ENCHANTED_BOOK || mat == Material.NAME_TAG || mat == Material.BOW || mat == Material.SADDLE) {
                points = 10;
                label = "Treasure";
            }

            activeSession.addPoints(player, points, label);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        MiniEventSession activeSession = eventManager.getActiveSession();
        if (activeSession == null || !activeSession.isActive()) {
            return;
        }

        Player player = event.getPlayer();
        Material mat = event.getBlock().getType();
        MiniEventType activeType = activeSession.getType();

        if (activeType == MiniEventType.ORE_RUSH) {
            int pts = 0;
            String name = "";

            if (mat == Material.COAL_ORE || mat == Material.DEEPSLATE_COAL_ORE || mat == Material.COPPER_ORE || mat == Material.DEEPSLATE_COPPER_ORE) {
                pts = 1;
                name = "Coal/Copper Ore";
            } else if (mat == Material.IRON_ORE || mat == Material.DEEPSLATE_IRON_ORE || mat == Material.LAPIS_ORE || mat == Material.DEEPSLATE_LAPIS_ORE) {
                pts = 2;
                name = "Iron/Lapis Ore";
            } else if (mat == Material.GOLD_ORE || mat == Material.DEEPSLATE_GOLD_ORE || mat == Material.REDSTONE_ORE || mat == Material.DEEPSLATE_REDSTONE_ORE) {
                pts = 3;
                name = "Gold/Redstone Ore";
            } else if (mat == Material.DIAMOND_ORE || mat == Material.DEEPSLATE_DIAMOND_ORE) {
                pts = 5;
                name = "Diamond Ore";
            } else if (mat == Material.ANCIENT_DEBRIS) {
                pts = 10;
                name = "Ancient Debris";
            }

            if (pts > 0) {
                activeSession.addPoints(player, pts, name);
            }
        } else if (activeType == MiniEventType.HARVEST_SPRINT) {
            BlockData blockData = event.getBlock().getBlockData();
            if (blockData instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
                int pts = 0;
                String name = "";

                if (mat == Material.WHEAT || mat == Material.CARROTS || mat == Material.POTATOES) {
                    pts = 1;
                    name = "Mature Crop";
                } else if (mat == Material.BEETROOTS) {
                    pts = 2;
                    name = "Beetroot";
                } else if (mat == Material.NETHER_WART) {
                    pts = 3;
                    name = "Nether Wart";
                }

                if (pts > 0) {
                    activeSession.addPoints(player, pts, name);
                }
            }
        } else if (activeType == MiniEventType.TREASURE_DIG) {
            if (mat == Material.DIRT || mat == Material.GRASS_BLOCK || mat == Material.SAND || mat == Material.GRAVEL) {
                int pts = mat == Material.DIRT || mat == Material.GRASS_BLOCK ? 1 : (mat == Material.SAND ? 2 : 3);
                activeSession.addPoints(player, pts, "Digging");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        MiniEventSession activeSession = eventManager.getActiveSession();
        if (activeSession == null || !activeSession.isActive() || activeSession.getType() != MiniEventType.MOB_FRENZY) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        int pts = 1;
        String typeName = event.getEntityType().name();
        switch (event.getEntityType()) {
            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
            case SLIME:
                pts = 2;
                break;
            case ENDERMAN:
            case WITCH:
            case GHAST:
            case BLAZE:
                pts = 5;
                break;
            case WITHER_SKELETON:
            case ELDER_GUARDIAN:
                pts = 10;
                break;
            case WARDEN:
            case WITHER:
            case ENDER_DRAGON:
                pts = 50;
                break;
            default:
                pts = 1;
                break;
        }

        activeSession.addPoints(killer, pts, typeName);
    }
}

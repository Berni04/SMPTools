package com.smp.smptools.events.minievents;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.EventManager;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.Set;

/**
 * Listens to player interactions and awards points, buffs, and offline rewards to active mini-event sessions.
 */
public class MiniEventListener implements Listener {

    private final SMPTools plugin;
    private final EventManager eventManager;
    private final Random random = new Random();

    private static final Set<Material> TREASURE_FISH_ITEMS = Set.of(
            Material.ENCHANTED_BOOK, Material.NAME_TAG, Material.BOW, Material.SADDLE,
            Material.FISHING_ROD, Material.NAUTILUS_SHELL
    );

    private static final Set<Material> JUNK_FISH_ITEMS = Set.of(
            Material.LILY_PAD, Material.BAMBOO, Material.BOWL, Material.LEATHER,
            Material.LEATHER_BOOTS, Material.ROTTEN_FLESH, Material.STICK,
            Material.STRING, Material.POTION, Material.BONE, Material.INK_SAC,
            Material.TRIPWIRE_HOOK
    );

    public MiniEventListener(SMPTools plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        eventManager.claimOfflineRewards(event.getPlayer());
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
            FileConfiguration cfg = plugin.getEventsConfig();

            int points = 0;
            String label = "";

            if (mat == Material.COD) {
                points = cfg.getInt("events.types.fishing_derby.points.cod", 1);
                label = "Cod";
            } else if (mat == Material.SALMON) {
                points = cfg.getInt("events.types.fishing_derby.points.salmon", 2);
                label = "Salmon";
            } else if (mat == Material.TROPICAL_FISH) {
                points = cfg.getInt("events.types.fishing_derby.points.tropical", 3);
                label = "Tropical Fish";
            } else if (mat == Material.PUFFERFISH) {
                points = cfg.getInt("events.types.fishing_derby.points.pufferfish", 5);
                label = "Pufferfish";
            } else if (TREASURE_FISH_ITEMS.contains(mat)) {
                points = cfg.getInt("events.types.fishing_derby.points.treasure", 10);
                label = "Treasure (" + mat.name() + ")";
            } else if (JUNK_FISH_ITEMS.contains(mat)) {
                points = 0;
                label = "Junk";
            } else {
                points = cfg.getInt("events.types.fishing_derby.points.cod", 1);
                label = "Catch";
            }

            if (points > 0) {
                activeSession.addPoints(player, points, label);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        MiniEventSession activeSession = eventManager.getActiveSession();
        if (activeSession == null || !activeSession.isActive()) {
            return;
        }
        event.getBlock().setMetadata("placed_by_player", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (com.smp.smptools.artifacts.ArtifactListener.isFelling(event.getBlock())) {
            return;
        }

        if (event.getBlock().hasMetadata("placed_by_player")) {
            event.getBlock().removeMetadata("placed_by_player", plugin);
            return;
        }

        MiniEventSession activeSession = eventManager.getActiveSession();
        if (activeSession == null || !activeSession.isActive()) {
            return;
        }

        Player player = event.getPlayer();
        Material mat = event.getBlock().getType();
        MiniEventType activeType = activeSession.getType();
        FileConfiguration cfg = plugin.getEventsConfig();

        if (activeType == MiniEventType.ORE_RUSH) {
            int pts = 0;
            String name = "";

            if (mat == Material.COAL_ORE || mat == Material.DEEPSLATE_COAL_ORE) {
                pts = cfg.getInt("events.types.ore_rush.points.coal", 1);
                name = "Coal Ore";
            } else if (mat == Material.COPPER_ORE || mat == Material.DEEPSLATE_COPPER_ORE) {
                pts = cfg.getInt("events.types.ore_rush.points.copper", 1);
                name = "Copper Ore";
            } else if (mat == Material.IRON_ORE || mat == Material.DEEPSLATE_IRON_ORE) {
                pts = cfg.getInt("events.types.ore_rush.points.iron", 2);
                name = "Iron Ore";
            } else if (mat == Material.LAPIS_ORE || mat == Material.DEEPSLATE_LAPIS_ORE) {
                pts = cfg.getInt("events.types.ore_rush.points.lapis", 2);
                name = "Lapis Ore";
            } else if (mat == Material.GOLD_ORE || mat == Material.DEEPSLATE_GOLD_ORE || mat == Material.NETHER_GOLD_ORE) {
                pts = cfg.getInt("events.types.ore_rush.points.gold", 3);
                name = "Gold Ore";
            } else if (mat == Material.REDSTONE_ORE || mat == Material.DEEPSLATE_REDSTONE_ORE) {
                pts = cfg.getInt("events.types.ore_rush.points.redstone", 3);
                name = "Redstone Ore";
            } else if (mat == Material.DIAMOND_ORE || mat == Material.DEEPSLATE_DIAMOND_ORE) {
                pts = cfg.getInt("events.types.ore_rush.points.diamond", 5);
                name = "Diamond Ore";
            } else if (mat == Material.ANCIENT_DEBRIS) {
                pts = cfg.getInt("events.types.ore_rush.points.ancient_debris", 10);
                name = "Ancient Debris";
            }

            if (pts > 0) {
                activeSession.addPoints(player, pts, name);

                // Double Ore Drops Buff (only if block drops items)
                if (event.isDropItems() && cfg.getBoolean("events.types.ore_rush.buffs.double-ore-drops", true)) {
                    for (ItemStack drop : event.getBlock().getDrops(player.getInventory().getItemInMainHand())) {
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                    }
                }
            }
        } else if (activeType == MiniEventType.HARVEST_SPRINT) {
            BlockData blockData = event.getBlock().getBlockData();
            if (blockData instanceof Ageable ageable && ageable.getAge() == ageable.getMaximumAge()) {
                int pts = 0;
                String name = "";

                if (mat == Material.WHEAT) {
                    pts = cfg.getInt("events.types.harvest_sprint.points.wheat", 1);
                    name = "Wheat";
                } else if (mat == Material.CARROTS) {
                    pts = cfg.getInt("events.types.harvest_sprint.points.carrot", 1);
                    name = "Carrot";
                } else if (mat == Material.POTATOES) {
                    pts = cfg.getInt("events.types.harvest_sprint.points.potato", 1);
                    name = "Potato";
                } else if (mat == Material.BEETROOTS) {
                    pts = cfg.getInt("events.types.harvest_sprint.points.beetroot", 2);
                    name = "Beetroot";
                } else if (mat == Material.NETHER_WART) {
                    pts = cfg.getInt("events.types.harvest_sprint.points.nether_wart", 3);
                    name = "Nether Wart";
                }

                if (pts > 0) {
                    activeSession.addPoints(player, pts, name);

                    // Speed Boost Buff
                    if (cfg.getBoolean("events.types.harvest_sprint.buffs.speed-boost", true)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, false));
                    }
                }
            }
        } else if (activeType == MiniEventType.TREASURE_DIG) {
            int pts = 0;
            if (mat == Material.DIRT || mat == Material.GRASS_BLOCK || mat == Material.COARSE_DIRT || mat == Material.ROOTED_DIRT) {
                pts = cfg.getInt("events.types.treasure_dig.points.dirt", 1);
            } else if (mat == Material.SAND || mat == Material.RED_SAND) {
                pts = cfg.getInt("events.types.treasure_dig.points.sand", 2);
            } else if (mat == Material.GRAVEL || mat == Material.CLAY) {
                pts = cfg.getInt("events.types.treasure_dig.points.gravel", 3);
            }

            if (pts > 0) {
                activeSession.addPoints(player, pts, "Digging");

                // Treasure Pouch Chance
                double pouchChance = cfg.getDouble("events.types.treasure_dig.treasure-pouch-chance", 0.05);
                if (random.nextDouble() < pouchChance) {
                    ItemStack treasure = new ItemStack(random.nextBoolean() ? Material.DIAMOND : Material.EMERALD, random.nextInt(3) + 1);
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), treasure);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        MiniEventSession activeSession = eventManager.getActiveSession();
        if (activeSession == null || !activeSession.isActive() || activeSession.getType() != MiniEventType.MOB_FRENZY) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        // Ensure killed entity is a hostile mob (not peaceful animal, villager, pet, or player)
        if (!(entity instanceof Monster || entity instanceof Boss || entity instanceof Slime ||
              entity instanceof Ghast || entity instanceof Phantom || entity instanceof Shulker ||
              entity instanceof Hoglin || entity instanceof Zoglin)) {
            return;
        }

        FileConfiguration cfg = plugin.getEventsConfig();
        int pts = 0;
        String typeName = entity.getType().name();

        switch (entity.getType()) {
            case ZOMBIE:
            case SKELETON:
            case DROWNED:
            case HUSK:
            case STRAY:
            case SILVERFISH:
            case ENDERMITE:
                pts = cfg.getInt("events.types.mob_frenzy.points.weak_mobs", 1);
                break;

            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
            case SLIME:
            case MAGMA_CUBE:
            case PHANTOM:
                pts = cfg.getInt("events.types.mob_frenzy.points.medium_mobs", 2);
                break;

            case ENDERMAN:
            case WITCH:
            case GHAST:
            case BLAZE:
            case PIGLIN_BRUTE:
            case SHULKER:
            case HOGLIN:
            case ZOGLIN:
                pts = cfg.getInt("events.types.mob_frenzy.points.strong_mobs", 5);
                break;

            case WITHER_SKELETON:
            case ELDER_GUARDIAN:
            case EVOKER:
            case RAVAGER:
                pts = cfg.getInt("events.types.mob_frenzy.points.elite_mobs", 10);
                break;

            case WARDEN:
            case WITHER:
            case ENDER_DRAGON:
                pts = cfg.getInt("events.types.mob_frenzy.points.boss_mobs", 50);
                break;

            default:
                pts = cfg.getInt("events.types.mob_frenzy.points.weak_mobs", 1);
                break;
        }

        if (pts > 0) {
            activeSession.addPoints(killer, pts, typeName);

            // Double Mob Drops Buff
            if (cfg.getBoolean("events.types.mob_frenzy.buffs.double-mob-drops", true)) {
                for (ItemStack drop : event.getDrops()) {
                    entity.getWorld().dropItemNaturally(entity.getLocation(), drop.clone());
                }
            }
        }
    }
}

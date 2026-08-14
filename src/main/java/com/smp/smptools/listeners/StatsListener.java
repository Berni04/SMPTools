package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Statistic;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class StatsListener implements Listener {

    private final SMPTools plugin;
    private final Random random = new Random();

    public StatsListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String uuid = player.getUniqueId().toString();

        // Increment total death count
        plugin.getStatsConfig().set("stats." + uuid + ".deaths_total",
                plugin.getStatsConfig().getInt("stats." + uuid + ".deaths_total", 0) + 1);

        // Save detailed death info
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + uuid + ".deaths_info");
        Map<String, Object> death = new HashMap<>();
        death.put("time", java.time.LocalDateTime.now().toString());
        death.put("world", player.getWorld().getName());
        death.put("x", player.getLocation().getBlockX());
        death.put("y", player.getLocation().getBlockY());
        death.put("z", player.getLocation().getBlockZ());
        death.put("cause", event.getDeathMessage());

        List<Map<String, Object>> inventory = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                inventory.add(item.serialize());
            } else {
                inventory.add(null);
            }
        }
        death.put("inventory", inventory);

        deathInfo.add(death);
        plugin.getStatsConfig().set("stats." + uuid + ".deaths_info", deathInfo);

        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".player_kills",
                    plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".player_kills", 0) + 1);
            if (plugin.getTagManager() != null) {
                plugin.getTagManager().checkMilestones(killer);
            }
        }
        if (plugin.getTagManager() != null) {
            plugin.getTagManager().checkMilestones(player);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            EntityType entityType = event.getEntityType();

            // Boss Kills
            if (entityType == EntityType.ENDER_DRAGON) {
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".ender_dragon_kills",
                        plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".ender_dragon_kills", 0) + 1);
            } else if (entityType == EntityType.WITHER) {
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".wither_kills",
                        plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".wither_kills", 0) + 1);
            } else if (entityType == EntityType.WARDEN) {
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".warden_kills",
                        plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".warden_kills", 0) + 1);
            }

            List<EntityType> trackedMobs = Arrays.asList(
                    EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.TURTLE,
                    EntityType.LLAMA, EntityType.RABBIT,
                    EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.ENDERMAN, EntityType.WITCH,
                    EntityType.BLAZE, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.PHANTOM, EntityType.SLIME,
                    EntityType.WITHER_SKELETON);

            if (trackedMobs.contains(entityType)) {
                String mobName = entityType.name().toLowerCase();
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".mob_kills." + mobName,
                        plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".mob_kills." + mobName, 0)
                                + 1);
            }
            // plugin.saveStatsConfig(); // Removed to use periodic saver
            if (plugin.getTagManager() != null) {
                plugin.getTagManager().checkMilestones(killer);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Increment total blocks broken
        plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".blocks_broken",
                plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".blocks_broken", 0) + 1);

        List<Material> diamondOres = Arrays.asList(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE);
        List<Material> goldOres = Arrays.asList(Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE);
        List<Material> ironOres = Arrays.asList(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
        List<Material> copperOres = Arrays.asList(Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
        List<Material> redstoneOres = Arrays.asList(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE);
        List<Material> lapisOres = Arrays.asList(Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE);
        List<Material> coalOres = Arrays.asList(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE);
        List<Material> emeraldOres = Arrays.asList(Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);

        String oreName = null;
        if (diamondOres.contains(blockType))
            oreName = "diamond";
        else if (goldOres.contains(blockType))
            oreName = "gold";
        else if (ironOres.contains(blockType))
            oreName = "iron";
        else if (copperOres.contains(blockType))
            oreName = "copper";
        else if (redstoneOres.contains(blockType))
            oreName = "redstone";
        else if (lapisOres.contains(blockType))
            oreName = "lapis";
        else if (coalOres.contains(blockType))
            oreName = "coal";
        else if (emeraldOres.contains(blockType))
            oreName = "emerald";

        if (oreName != null) {
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".ores_mined." + oreName,
                    plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".ores_mined." + oreName, 0) + 1);
        }
        if (plugin.getTagManager() != null) {
            plugin.getTagManager().checkMilestones(player);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".blocks_placed",
                plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".blocks_placed", 0) + 1);
        if (plugin.getTagManager() != null) {
            plugin.getTagManager().checkMilestones(player);
        }
    }

    @EventHandler
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() == Statistic.PLAY_ONE_MINUTE) {
            Player player = event.getPlayer();
            long totalTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long totalMinutes = totalTicks / (20 * 60);
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".playtime_minutes", totalMinutes);
            if (plugin.getTagManager() != null) {
                plugin.getTagManager().checkMilestones(player);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equals("world_nether")) {
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".enter_nether", 1);
            if (plugin.getTagManager() != null) {
                plugin.getTagManager().checkMilestones(player);
            }
        }
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Material itemType = event.getItem().getItemStack().getType();

            if (itemType == Material.ELYTRA) {
                plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".obtain_elytra", 1);
            } else if (itemType == Material.TRIDENT) {
                plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".obtain_trident", 1);
            } else {
                return; // Don't check milestones if the item is not relevant
            }
            if (plugin.getTagManager() != null) {
                plugin.getTagManager().checkMilestones(player);
            }
        }
    }

    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".use_totem",
                    plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".use_totem", 0) + 1);
            if (plugin.getTagManager() != null) {
                plugin.getTagManager().checkMilestones(player);
            }
        }
    }
}

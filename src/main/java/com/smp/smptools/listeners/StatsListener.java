package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.ChatColor;
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

        // Funny Death Messages
        if (plugin.getConfig().getBoolean("features.funny-death-messages.enabled", true)) {
            handleFunnyDeathMessage(event);
        }

        // Increment total death count
        plugin.getStatsConfig().set("stats." + uuid + ".deaths_total", plugin.getStatsConfig().getInt("stats." + uuid + ".deaths_total", 0) + 1);

        // Save detailed death info
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList("stats." + uuid + ".deaths_info");
        Map<String, Object> death = new HashMap<>();
        death.put("time", java.time.LocalDateTime.now().toString());
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
            plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".player_kills", plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".player_kills", 0) + 1);
            plugin.getTagManager().checkMilestones(killer);
        }
        plugin.saveStatsConfig();
        plugin.getTagManager().checkMilestones(player);
    }

    private void handleFunnyDeathMessage(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName();
        String deathMessage;

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage == null) {
            deathMessage = getRandomMessage(Arrays.asList(
                "%player% ceased to exist.",
                "%player% went gentle into that good night.",
                "%player%'s story ends here."
            ));
        } else {
            switch (lastDamage.getCause()) {
                case ENTITY_ATTACK:
                    if (lastDamage instanceof EntityDamageByEntityEvent) {
                        Entity damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
                        if (damager instanceof Player) {
                            deathMessage = getRandomMessage(Arrays.asList(
                                "%player% was sent back to the lobby by %killer%.",
                                "%player% learned that %killer% is not their friend.",
                                "%player% was outplayed by %killer%."
                            )).replace("%killer%", ((Player) damager).getName());
                        } else {
                            deathMessage = getRandomMessage(Arrays.asList(
                                "%player% was slain by a " + damager.getType().name().toLowerCase() + ".",
                                "A " + damager.getType().name().toLowerCase() + " had a bone to pick with %player%."
                            ));
                        }
                    } else {
                        deathMessage = "%player% was killed by something.";
                    }
                    break;
                case ENTITY_EXPLOSION:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% got a hug from a Creeper.",
                        "A Creeper whispered sweet nothings into %player%'s ear. Sssss...",
                        "%player% learned that some hugs are explosive."
                    ));
                    break;
                case BLOCK_EXPLOSION:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% should not have slept in the Nether.",
                        "%player%'s bed went boom.",
                        "%player% learned about explosive interior design."
                    ));
                    break;
                case FALL:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% thought they were a bird.",
                        "%player% forgot to deploy their parachute.",
                        "%player% tested gravity. It still works.",
                        "It wasn't the fall that killed %player%, it was the sudden stop."
                    ));
                    break;
                case LAVA:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% tried to swim in the forbidden soup.",
                        "%player% thought lava was just spicy water.",
                        "%player% is now one with the magma."
                    ));
                    break;
                case DROWNING:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% forgot how to breathe.",
                        "%player% is sleeping with the fishes.",
                        "%player% discovered they are not a submarine."
                    ));
                    break;
                case VOID:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% fell out of the world.",
                        "%player% clipped into the backrooms.",
                        "%player% has been deleted from the simulation."
                    ));
                    break;
                case FIRE:
                case FIRE_TICK:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% is extra crispy now.",
                        "%player% forgot to stop, drop, and roll.",
                        "%player% tried to be a firebender, but failed."
                    ));
                    break;
                case PROJECTILE:
                     deathMessage = getRandomMessage(Arrays.asList(
                        "%player% was pincushioned.",
                        "%player% tried to catch an arrow with their face.",
                        "%player% was impaled."
                    ));
                    break;
                case SUFFOCATION:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% became one with the wall.",
                        "%player% is experiencing claustrophobia, permanently."
                    ));
                    break;
                case WITHER:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% withered away.",
                        "%player% didn't feel so good."
                    ));
                    break;
                default:
                    deathMessage = getRandomMessage(Arrays.asList(
                        "%player% died in a mysterious way.",
                        "%player% met their end.",
                        "So long, %player%, and thanks for all the fish."
                    ));
                    break;
            }
        }

        event.setDeathMessage(ChatColor.RED + deathMessage.replace("%player%", playerName));
    }

    private String getRandomMessage(List<String> messages) {
        return messages.get(random.nextInt(messages.size()));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            EntityType entityType = event.getEntityType();

            // Boss Kills
            if (entityType == EntityType.ENDER_DRAGON) {
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".ender_dragon_kills", plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".ender_dragon_kills", 0) + 1);
            } else if (entityType == EntityType.WITHER) {
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".wither_kills", plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".wither_kills", 0) + 1);
            } else if (entityType == EntityType.WARDEN) {
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".warden_kills", plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".warden_kills", 0) + 1);
            }

            List<EntityType> trackedMobs = Arrays.asList(
                    EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN, EntityType.TURTLE, EntityType.LLAMA, EntityType.RABBIT,
                    EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.ENDERMAN, EntityType.WITCH, EntityType.BLAZE, EntityType.SPIDER, EntityType.CAVE_SPIDER, EntityType.PHANTOM, EntityType.SLIME, EntityType.WITHER_SKELETON
            );

            if (trackedMobs.contains(entityType)) {
                String mobName = entityType.name().toLowerCase();
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".mob_kills." + mobName, plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".mob_kills." + mobName, 0) + 1);
            }
            plugin.saveStatsConfig();
            plugin.getTagManager().checkMilestones(killer);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

        // Increment total blocks broken
        plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".blocks_broken", plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".blocks_broken", 0) + 1);

        List<Material> diamondOres = Arrays.asList(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE);
        List<Material> goldOres = Arrays.asList(Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE);
        List<Material> ironOres = Arrays.asList(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
        List<Material> copperOres = Arrays.asList(Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
        List<Material> redstoneOres = Arrays.asList(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE);
        List<Material> lapisOres = Arrays.asList(Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE);
        List<Material> coalOres = Arrays.asList(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE);
        List<Material> emeraldOres = Arrays.asList(Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);

        String oreName = null;
        if (diamondOres.contains(blockType)) oreName = "diamond";
        else if (goldOres.contains(blockType)) oreName = "gold";
        else if (ironOres.contains(blockType)) oreName = "iron";
        else if (copperOres.contains(blockType)) oreName = "copper";
        else if (redstoneOres.contains(blockType)) oreName = "redstone";
        else if (lapisOres.contains(blockType)) oreName = "lapis";
        else if (coalOres.contains(blockType)) oreName = "coal";
        else if (emeraldOres.contains(blockType)) oreName = "emerald";

        if (oreName != null) {
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".ores_mined." + oreName, plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".ores_mined." + oreName, 0) + 1);
        }
        plugin.saveStatsConfig();
        plugin.getTagManager().checkMilestones(player);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".blocks_placed", plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".blocks_placed", 0) + 1);
        plugin.saveStatsConfig();
        plugin.getTagManager().checkMilestones(player);
    }

    @EventHandler
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        if (event.getStatistic() == Statistic.PLAY_ONE_MINUTE) {
            Player player = event.getPlayer();
            long totalTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long totalMinutes = totalTicks / (20 * 60);
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".playtime_minutes", totalMinutes);
            plugin.saveStatsConfig();
            plugin.getTagManager().checkMilestones(player);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getWorld().getName().equals("world_nether")) {
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".enter_nether", 1);
            plugin.saveStatsConfig();
            plugin.getTagManager().checkMilestones(player);
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
            plugin.saveStatsConfig();
            plugin.getTagManager().checkMilestones(player);
        }
    }

    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".use_totem", plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".use_totem", 0) + 1);
            plugin.saveStatsConfig();
            plugin.getTagManager().checkMilestones(player);
        }
    }
}

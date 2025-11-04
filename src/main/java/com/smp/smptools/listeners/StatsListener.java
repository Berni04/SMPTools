package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Arrays;
import java.util.List;

public class StatsListener implements Listener {

    private final SMPTools plugin;

    public StatsListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getStatsConfig().set("stats." + player.getUniqueId() + ".deaths", plugin.getStatsConfig().getInt("stats." + player.getUniqueId() + ".deaths", 0) + 1);

        if (player.getKiller() != null) {
            Player killer = player.getKiller();
            plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".player_kills", plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".player_kills", 0) + 1);
        }
        plugin.saveStatsConfig();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            EntityType entityType = event.getEntityType();

            List<EntityType> trackedMobs = Arrays.asList(
                    EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
                    EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.ENDERMAN
            );

            if (trackedMobs.contains(entityType)) {
                String mobName = entityType.name().toLowerCase();
                plugin.getStatsConfig().set("stats." + killer.getUniqueId() + ".mob_kills." + mobName, plugin.getStatsConfig().getInt("stats." + killer.getUniqueId() + ".mob_kills." + mobName, 0) + 1);
                plugin.saveStatsConfig();
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();

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
            plugin.saveStatsConfig();
        }
    }
}

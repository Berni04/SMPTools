package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.skills.SkillType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Arrays;
import java.util.List;

public class SkillsListener implements Listener {

    private final SMPTools plugin;

    public SkillsListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    private static final List<Material> MINING_BLOCKS = Arrays.asList(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    private static final List<Material> WOODCUTTING_BLOCKS = Arrays.asList(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_LOG
    );

    private static final List<Material> EXCAVATION_BLOCKS = Arrays.asList(
            Material.DIRT, Material.GRASS_BLOCK, Material.SAND, Material.GRAVEL,
            Material.CLAY, Material.SOUL_SAND, Material.SOUL_SOIL
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();
        SkillType skill = null;

        if (MINING_BLOCKS.contains(type)) {
            skill = SkillType.MINING;
        } else if (WOODCUTTING_BLOCKS.contains(type)) {
            skill = SkillType.WOODCUTTING;
        } else if (EXCAVATION_BLOCKS.contains(type)) {
            skill = SkillType.EXCAVATION;
        }

        if (skill != null) {
            plugin.getSkillsManager().addExperience(event.getPlayer(), skill, 1);

            if (plugin.getSkillsManager().attemptDoubleDrop(event.getPlayer(), skill)) {
                block.getWorld().dropItemNaturally(block.getLocation(), new org.bukkit.inventory.ItemStack(type, 1));
            }
        }
    }
}

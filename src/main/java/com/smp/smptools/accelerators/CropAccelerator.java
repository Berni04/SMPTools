package com.smp.smptools.accelerators;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CropAccelerator extends BukkitRunnable {

    private final SMPTools plugin;
    private final double multiplier;
    private final Random random = new Random();

    // List of materials that are ageable crops
    private static final List<Material> AGEABLE_CROPS = Arrays.asList(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART,
            Material.COCOA_BEANS, Material.PUMPKIN_STEM, Material.MELON_STEM, Material.SUGAR_CANE, Material.CACTUS,
            Material.BAMBOO, Material.KELP_PLANT, Material.KELP
    );

    // List of materials that are saplings
    private static final List<Material> SAPLINGS = Arrays.asList(
            Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING, Material.JUNGLE_SAPLING,
            Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING, Material.MANGROVE_PROPAGULE, Material.CHERRY_SAPLING
    );

    public CropAccelerator(SMPTools plugin, double multiplier) {
        this.plugin = plugin;
        this.multiplier = multiplier;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("features.accelerated-growth.enabled", true)) {
            this.cancel(); // Stop the task if feature is disabled
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                // Apply random ticks based on multiplier
                for (int i = 0; i < multiplier; i++) { // Apply 'multiplier' number of random ticks
                    int x = random.nextInt(16);
                    int y = random.nextInt(world.getMaxHeight()); // Max height can vary
                    int z = random.nextInt(16);

                    Block block = chunk.getBlock(x, y, z);
                    BlockData blockData = block.getBlockData();

                    if (blockData instanceof Ageable) {
                        Ageable ageable = (Ageable) blockData;
                        if (ageable.getAge() < ageable.getMaximumAge()) {
                            int newAge = Math.min(ageable.getMaximumAge(), ageable.getAge() + 1); // Increment age by 1 per "tick"
                            ageable.setAge(newAge);
                            block.setBlockData(ageable);
                        }
                    } else if (SAPLINGS.contains(block.getType())) {
                        block.applyBoneMeal(BlockFace.UP); // Simulate bonemeal for sapling growth
                    } else if (AGEABLE_CROPS.contains(block.getType())) {
                        // For non-ageable crops that grow (like sugar cane, cactus, bamboo)
                        // applyBoneMeal might not work directly, but a random tick can still trigger growth.
                        // For now, applyBoneMeal is a good general approach.
                        block.applyBoneMeal(BlockFace.UP);
                    }
                }
            }
        }
    }
}

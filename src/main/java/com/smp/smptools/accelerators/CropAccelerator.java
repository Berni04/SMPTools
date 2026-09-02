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
            Material.COCOA, Material.PUMPKIN_STEM, Material.MELON_STEM, Material.SUGAR_CANE, Material.CACTUS,
            Material.BAMBOO, Material.KELP_PLANT, Material.KELP, Material.SWEET_BERRY_BUSH, Material.TORCHFLOWER_CROP, Material.PITCHER_CROP
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

        long rounded = (long) Math.min((double) Long.MAX_VALUE, Math.max(0.0, Math.round(multiplier)));
        int clampedMultiplier = (int) Math.min(64L, Math.max(0L, rounded));
        if (clampedMultiplier <= 0) return;

        for (World world : Bukkit.getWorlds()) {
            int minHeight = world.getMinHeight();

            for (Chunk chunk : world.getLoadedChunks()) {
                if (!chunk.isLoaded()) continue;

                int chunkBaseX = chunk.getX() << 4;
                int chunkBaseZ = chunk.getZ() << 4;

                // Apply random ticks based on multiplier
                for (int i = 0; i < clampedMultiplier; i++) {
                    int x = random.nextInt(16);
                    int z = random.nextInt(16);
                    int highestY = world.getHighestBlockYAt(chunkBaseX + x, chunkBaseZ + z);
                    if (highestY < minHeight) continue;

                    int minY = Math.max(minHeight, highestY - 32);
                    int maxY = Math.min(world.getMaxHeight() - 1, highestY + 1);
                    int y = minY + (maxY > minY ? random.nextInt(maxY - minY + 1) : 0);

                    Block block = chunk.getBlock(x, y, z);
                    Material mat = block.getType();

                    if (!AGEABLE_CROPS.contains(mat) && !SAPLINGS.contains(mat)) {
                        continue;
                    }

                    BlockData blockData = block.getBlockData();

                    if (blockData instanceof Ageable ageable) {
                        if (ageable.getAge() < ageable.getMaximumAge()) {
                            int newAge = Math.min(ageable.getMaximumAge(), ageable.getAge() + 1);
                            ageable.setAge(newAge);
                            block.setBlockData(ageable);
                        }
                    } else if (SAPLINGS.contains(mat)) {
                        if (random.nextInt(10) == 0) {
                            block.applyBoneMeal(BlockFace.UP);
                        }
                    } else if (AGEABLE_CROPS.contains(mat)) {
                        block.applyBoneMeal(BlockFace.UP);
                    }
                }
            }
        }
    }
}

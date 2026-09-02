package com.smp.smptools.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.Set;

/**
 * Utility for finding and validating safe teleport destinations.
 * Prevents players from teleporting into walls, lava, fire, voids, or outside the world border.
 */
public final class SafeLocationFinder {

    private static final int[] VERTICAL_OFFSETS = {0, 1, -1, 2, -2, 3, -3};

    private static final Set<Material> HAZARDS = Set.of(
            Material.LAVA,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW,
            Material.WITHER_ROSE,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE
    );

    private static final Set<Material> UNSAFE_GROUND = Set.of(
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.SWEET_BERRY_BUSH,
            Material.POWDER_SNOW,
            Material.WITHER_ROSE
    );

    private SafeLocationFinder() {
        // Utility class
    }

    /**
     * Finds a safe location near the target location, preserving yaw and pitch.
     *
     * @param target the target location
     * @return an Optional containing a safe Location, or empty if no safe spot was found
     */
    public static Optional<Location> findSafeLocation(Location target) {
        if (target == null || target.getWorld() == null) {
            return Optional.empty();
        }

        World world = target.getWorld();
        WorldBorder border = world.getWorldBorder();
        if (!border.isInside(target)) {
            return Optional.empty();
        }

        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();

        int targetX = target.getBlockX();
        int targetY = target.getBlockY();
        int targetZ = target.getBlockZ();

        for (int dy : VERTICAL_OFFSETS) {
            int y = targetY + dy;
            if (y <= minHeight || y + 1 >= maxHeight) {
                continue;
            }

            Block feetBlock = world.getBlockAt(targetX, y, targetZ);
            Block headBlock = world.getBlockAt(targetX, y + 1, targetZ);
            Block groundBlock = world.getBlockAt(targetX, y - 1, targetZ);

            if (isPassable(feetBlock) && isPassable(headBlock) && isSafeGround(groundBlock)) {
                Location safeLoc = new Location(
                        world,
                        targetX + 0.5,
                        y,
                        targetZ + 0.5,
                        target.getYaw(),
                        target.getPitch()
                );
                return Optional.of(safeLoc);
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a location is completely safe for a player to occupy.
     *
     * @param location the location to check
     * @return true if safe
     */
    public static boolean isSafe(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        World world = location.getWorld();
        int y = location.getBlockY();
        if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
            return false;
        }

        Block feet = world.getBlockAt(location.getBlockX(), y, location.getBlockZ());
        Block head = world.getBlockAt(location.getBlockX(), y + 1, location.getBlockZ());
        Block ground = world.getBlockAt(location.getBlockX(), y - 1, location.getBlockZ());

        return isPassable(feet) && isPassable(head) && isSafeGround(ground) && world.getWorldBorder().isInside(location);
    }

    private static boolean isPassable(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (HAZARDS.contains(type)) return false;
        if (block.isLiquid()) return false;
        return block.isPassable();
    }

    private static boolean isSafeGround(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (UNSAFE_GROUND.contains(type)) return false;
        if (block.isLiquid()) return false;
        return type.isSolid();
    }
}

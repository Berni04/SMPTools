package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PortalListener implements Listener {

    private final SMPTools plugin;

    public PortalListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.WATER_BUCKET)
            return;

        Block clickedBlock = event.getBlockClicked();

        // We check if the user clicked on the bottom frame with a water bucket
        if (isPortalFrame(clickedBlock)) {
            event.setCancelled(true);
            // Fill with powder snow to look like a portal
            fillPortal(clickedBlock);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null)
            return;

        Block block = to.getBlock();
        if (block.getType() == Material.POWDER_SNOW) {
            // Check if this powder snow is inside a valid portal frame
            if (isInsidePortal(block)) {
                Player player = event.getPlayer();

                // Teleport logic
                if (player.getWorld().getName().equals("christmas")) {
                    // Teleport back to overworld spawn (or previous location if we tracked it)
                    player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                } else {
                    // Teleport to Christmas world
                    Location target = plugin.getPortalManager().getChristmasSpawn();
                    if (target != null) {
                        player.teleport(target);
                    }
                }
            }
        }
    }

    private boolean isInsidePortal(Block powderSnow) {
        // Check all possible frame orientations around this powder snow block
        BlockFace[] horizontalFaces = { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };

        for (BlockFace face : horizontalFaces) {
            // Check if there's a snow block frame in this direction
            Block adjacentBlock = powderSnow.getRelative(face);
            if (adjacentBlock.getType() == Material.SNOW_BLOCK) {
                // This could be part of a frame, check if it's a valid portal
                // Try checking from the bottom of the portal
                Block bottomFrame = powderSnow.getRelative(BlockFace.DOWN);
                if (bottomFrame.getType() == Material.SNOW_BLOCK) {
                    if (checkFrame(bottomFrame, face) || checkFrame(bottomFrame, face.getOppositeFace())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isPortalFrame(Block center) {
        // Check if we are clicking on the bottom frame of a potential portal
        if (center.getType() != Material.SNOW_BLOCK)
            return false;

        // We need to check for a 4x5 frame.
        // The clicked block could be one of the two bottom blocks.

        // Check East-West orientation
        if (checkFrame(center, BlockFace.EAST) || checkFrame(center, BlockFace.WEST))
            return true;

        // Check North-South orientation
        if (checkFrame(center, BlockFace.NORTH) || checkFrame(center, BlockFace.SOUTH))
            return true;

        return false;
    }

    private boolean checkFrame(Block bottom, BlockFace side) {
        // We assume 'bottom' is one of the two bottom blocks. 'side' is the direction
        // to the other bottom block.
        Block otherBottom = bottom.getRelative(side);
        if (otherBottom.getType() != Material.SNOW_BLOCK)
            return false;

        // Check columns
        // Left column (relative to 'side')
        BlockFace up = BlockFace.UP;
        Block leftColumnBase = bottom.getRelative(side.getOppositeFace());
        if (leftColumnBase.getType() != Material.SNOW_BLOCK)
            return false;

        for (int i = 1; i <= 3; i++) {
            if (leftColumnBase.getRelative(up, i).getType() != Material.SNOW_BLOCK)
                return false;
        }

        // Right column
        Block rightColumnBase = otherBottom.getRelative(side);
        if (rightColumnBase.getType() != Material.SNOW_BLOCK)
            return false;

        for (int i = 1; i <= 3; i++) {
            if (rightColumnBase.getRelative(up, i).getType() != Material.SNOW_BLOCK)
                return false;
        }

        // Top row
        Block topLeft = leftColumnBase.getRelative(up, 4);
        Block topRight = rightColumnBase.getRelative(up, 4);

        if (topLeft.getType() != Material.SNOW_BLOCK)
            return false;
        if (topRight.getType() != Material.SNOW_BLOCK)
            return false;

        // Check top middle blocks
        if (topLeft.getRelative(side).getType() != Material.SNOW_BLOCK)
            return false;
        if (topLeft.getRelative(side, 2).getType() != Material.SNOW_BLOCK)
            return false;

        return true;
    }

    private void fillPortal(Block clickedBlock) {
        // We need to find the inside area again to fill it.
        // Since we validated it, we just need to find the orientation again.

        BlockFace[] faces = { BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH };
        for (BlockFace face : faces) {
            if (checkFrame(clickedBlock, face)) {
                // Fill the 2x3 area
                Block bottom1 = clickedBlock;
                Block bottom2 = clickedBlock.getRelative(face);

                for (int y = 1; y <= 3; y++) {
                    bottom1.getRelative(BlockFace.UP, y).setType(Material.POWDER_SNOW);
                    bottom2.getRelative(BlockFace.UP, y).setType(Material.POWDER_SNOW);
                }
                return;
            }
        }
    }
}

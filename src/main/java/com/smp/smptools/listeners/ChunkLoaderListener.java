package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.chunkloaders.ChunkLoaderManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class ChunkLoaderListener implements Listener {

    private final SMPTools plugin;
    private final ChunkLoaderManager chunkLoaderManager;

    public ChunkLoaderListener(SMPTools plugin) {
        this.plugin = plugin;
        this.chunkLoaderManager = plugin.getChunkLoaderManager();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();

        if (ChunkLoaderManager.isChunkLoaderItem(itemInHand)) {
            Block placedBlock = event.getBlockPlaced();
            chunkLoaderManager.addChunkLoader(placedBlock.getLocation());
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("chunk-loader.placed", player));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            return;
        }

        Block brokenBlock = event.getBlock();
        if (chunkLoaderManager.isChunkLoader(brokenBlock.getLocation())) {
            chunkLoaderManager.removeChunkLoader(brokenBlock.getLocation());
            event.getPlayer().sendMessage(SMPTools.getInstance().getMessageManager().getMessage("chunk-loader.removed", event.getPlayer()));
            // Optionally, drop the item back
            event.setDropItems(false); // Prevent default drop
            event.getBlock().getWorld().dropItemNaturally(brokenBlock.getLocation().add(0.5, 0.5, 0.5), ChunkLoaderManager.getChunkLoaderItem());
        }
    }
}

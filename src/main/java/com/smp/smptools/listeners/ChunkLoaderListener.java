package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.chunkloaders.ChunkLoaderManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ChunkLoaderListener implements Listener {

    private final SMPTools plugin;
    private final ChunkLoaderManager chunkLoaderManager;

    public ChunkLoaderListener(SMPTools plugin) {
        this.plugin = plugin;
        this.chunkLoaderManager = plugin.getChunkLoaderManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();

        if (ChunkLoaderManager.isChunkLoaderItem(itemInHand)) {
            if (!chunkLoaderManager.canPlaceChunkLoader(player.getUniqueId(), player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceMonitor(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItemInHand();

        if (ChunkLoaderManager.isChunkLoaderItem(itemInHand)) {
            Block placedBlock = event.getBlockPlaced();
            boolean added = chunkLoaderManager.addChunkLoader(placedBlock.getLocation(), player.getUniqueId(), player);
            if (added) {
                player.sendMessage(plugin.getMessageManager().getMessage("chunk-loader.placed", player));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            return;
        }

        Block brokenBlock = event.getBlock();
        Location loc = brokenBlock.getLocation();
        if (chunkLoaderManager.isChunkLoader(loc)) {
            Player player = event.getPlayer();
            UUID owner = chunkLoaderManager.getOwner(loc);
            if (owner != null) {
                if (!owner.equals(player.getUniqueId()) && !player.hasPermission("smptools.chunkloaders.admin")) {
                    event.setCancelled(true);
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>🔒 You cannot break someone else's chunk loader!</red>"));
                    return;
                }
            } else if (!player.hasPermission("smptools.chunkloaders.admin")) {
                // Fail-closed for legacy unowned chunk loaders: admin required
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>🔒 Only administrators can remove legacy chunk loaders without recorded ownership.</red>"));
                return;
            }

            event.setDropItems(false); // Prevent default block drop
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakMonitor(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("features.chunk-loaders.enabled", true)) {
            return;
        }

        Block brokenBlock = event.getBlock();
        Location loc = brokenBlock.getLocation();
        if (chunkLoaderManager.isChunkLoader(loc)) {
            Player player = event.getPlayer();
            chunkLoaderManager.removeChunkLoader(loc);
            player.sendMessage(plugin.getMessageManager().getMessage("chunk-loader.removed", player));
            brokenBlock.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), ChunkLoaderManager.getChunkLoaderItem());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            if (chunkLoaderManager.isChunkLoader(loc)) {
                chunkLoaderManager.removeChunkLoader(loc);
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), ChunkLoaderManager.getChunkLoaderItem());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            Location loc = block.getLocation();
            if (chunkLoaderManager.isChunkLoader(loc)) {
                chunkLoaderManager.removeChunkLoader(loc);
                loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), ChunkLoaderManager.getChunkLoaderItem());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Location loc = event.getBlock().getLocation();
        if (chunkLoaderManager.isChunkLoader(loc)) {
            chunkLoaderManager.removeChunkLoader(loc);
            loc.getWorld().dropItemNaturally(loc.clone().add(0.5, 0.5, 0.5), ChunkLoaderManager.getChunkLoaderItem());
        }
    }
}

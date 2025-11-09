package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.enchants.LumberjackEnchant;
import com.smp.smptools.enchants.TelekinesisEnchant;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class EnchantmentListener implements Listener {

    private final SMPTools plugin;
    private final Set<Block> brokenBlocks = new HashSet<>();

    public EnchantmentListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.getEnchantmentManager().hasEnchantment(tool, new TelekinesisEnchant())) {
            // Check if inventory is full
            if (event.getPlayer().getInventory().firstEmpty() == -1) {
                return; // Don't do anything, let the items drop normally
            }

            event.setCancelled(true); // Cancel the drop
            for (org.bukkit.entity.Item item : event.getItems()) {
                // Add items and drop any that don't fit
                for (ItemStack leftover : event.getPlayer().getInventory().addItem(item.getItemStack()).values()) {
                    event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), leftover);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (brokenBlocks.contains(event.getBlock())) {
            return; // This block was broken by our logic, so ignore it
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.getEnchantmentManager().hasEnchantment(tool, new LumberjackEnchant())) {
            if (isLog(event.getBlock().getType())) {
                breakTree(event.getPlayer(), event.getBlock(), tool); // Pass player and tool
            }
        }
    }

    private void breakTree(Player player, Block startBlock, ItemStack tool) {
        Set<Block> toBreak = new HashSet<>();
        collectLogs(startBlock, toBreak, startBlock.getType());

        boolean hasTelekinesis = plugin.getEnchantmentManager().hasEnchantment(tool, new TelekinesisEnchant());

        for (Block block : toBreak) {
            if (block.equals(startBlock)) {
                continue; // Don't re-break the starting block
            }
            brokenBlocks.add(block); // Mark as broken by us

            if (hasTelekinesis) {
                // If telekinesis is active, handle drops manually
                if (player.getInventory().firstEmpty() != -1) {
                    for (ItemStack drop : block.getDrops(tool)) {
                        for (ItemStack leftover : player.getInventory().addItem(drop).values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }
                    }
                    block.setType(Material.AIR); // Break block without drops
                } else {
                    // Inventory is full, break normally
                    block.breakNaturally(tool);
                }
            } else {
                // No telekinesis, break normally
                block.breakNaturally(tool);
            }
        }
        brokenBlocks.clear(); // Clean up for the next event
    }

    private void collectLogs(Block currentBlock, Set<Block> collected, Material logType) {
        if (currentBlock == null || !isLog(currentBlock.getType()) || currentBlock.getType() != logType || collected.contains(currentBlock)) {
            return;
        }

        collected.add(currentBlock);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    collectLogs(currentBlock.getRelative(x, y, z), collected, logType);
                }
            }
        }
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG");
    }
}

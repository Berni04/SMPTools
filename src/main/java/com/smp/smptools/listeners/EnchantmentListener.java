package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.enchants.LumberjackEnchant;
import com.smp.smptools.enchants.TelekinesisEnchant;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class EnchantmentListener implements Listener {

    private static final int MAX_LOGS_PER_BREAK = 512;
    private final SMPTools plugin;
    private final Set<Block> brokenBlocks = new HashSet<>();

    public EnchantmentListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.getEnchantmentManager().hasEnchantment(tool, new TelekinesisEnchant())) {
            if (event.getPlayer().getInventory().firstEmpty() == -1) {
                return;
            }

            event.setCancelled(true);
            for (org.bukkit.entity.Item item : event.getItems()) {
                for (ItemStack leftover : event.getPlayer().getInventory().addItem(item.getItemStack()).values()) {
                    event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), leftover);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (brokenBlocks.contains(event.getBlock())) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (plugin.getEnchantmentManager().hasEnchantment(tool, new LumberjackEnchant())) {
            if (isLog(event.getBlock().getType())) {
                breakTree(event.getPlayer(), event.getBlock(), tool);
            }
        }
    }

    private void breakTree(Player player, Block startBlock, ItemStack tool) {
        Set<Block> toBreak = collectLogsIterative(startBlock, startBlock.getType());
        boolean hasTelekinesis = plugin.getEnchantmentManager().hasEnchantment(tool, new TelekinesisEnchant());

        try {
            for (Block block : toBreak) {
                if (block.equals(startBlock)) {
                    continue;
                }
                brokenBlocks.add(block);

                BlockBreakEvent childEvent = new BlockBreakEvent(block, player);
                Bukkit.getPluginManager().callEvent(childEvent);
                if (childEvent.isCancelled()) {
                    continue;
                }

                if (hasTelekinesis) {
                    if (player.getInventory().firstEmpty() != -1) {
                        for (ItemStack drop : block.getDrops(tool)) {
                            for (ItemStack leftover : player.getInventory().addItem(drop).values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                            }
                        }
                        block.setType(Material.AIR);
                    } else {
                        block.breakNaturally(tool);
                    }
                } else {
                    block.breakNaturally(tool);
                }
            }
        } finally {
            brokenBlocks.clear();
        }
    }

    private Set<Block> collectLogsIterative(Block startBlock, Material logType) {
        Set<Block> collected = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();

        queue.add(startBlock);
        collected.add(startBlock);

        while (!queue.isEmpty() && collected.size() < MAX_LOGS_PER_BREAK) {
            Block current = queue.poll();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        Block neighbor = current.getRelative(x, y, z);
                        if (neighbor.getType() == logType && collected.add(neighbor)) {
                            queue.add(neighbor);
                            if (collected.size() >= MAX_LOGS_PER_BREAK) {
                                return collected;
                            }
                        }
                    }
                }
            }
        }

        return collected;
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_WOOD") || name.endsWith("_HYPHAE");
    }
}

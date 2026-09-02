package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.locks.LockManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LockListener implements Listener {

    private final SMPTools plugin;
    private final LockManager lockManager;

    public LockListener(SMPTools plugin) {
        this.plugin = plugin;
        this.lockManager = plugin.getLockManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !lockManager.isContainer(block)) return;

        Player player = event.getPlayer();
        if (lockManager.isLocked(block) && !lockManager.canAccess(block, player)) {
            event.setCancelled(true);
            String ownerName = lockManager.getOwnerName(block);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>🔒 This container is locked by " + ownerName + "!</red>"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!lockManager.isContainer(block)) return;

        Player player = event.getPlayer();
        if (lockManager.isLocked(block)) {
            if (!lockManager.canAccess(block, player)) {
                event.setCancelled(true);
                String ownerName = lockManager.getOwnerName(block);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>🔒 You cannot break a container locked by " + ownerName + "!</red>"));
                return;
            }

            Block survivingHalf = lockManager.getSurvivingDoubleChestHalf(block);
            if (survivingHalf != null) {
                lockManager.removeOrMigrateLock(block, survivingHalf);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Lock transferred to remaining chest half.</gray>"));
            } else {
                lockManager.removeLock(block);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Container unlocked.</gray>"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        Block sourceBlock = getBlockFromInventory(event.getSource());
        if (sourceBlock != null && lockManager.isContainer(sourceBlock) && lockManager.isLocked(sourceBlock)) {
            event.setCancelled(true);
            return;
        }

        Block destBlock = getBlockFromInventory(event.getDestination());
        if (destBlock != null && lockManager.isContainer(destBlock) && lockManager.isLocked(destBlock)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (block != null && lockManager.isContainer(block) && lockManager.isLocked(block)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> lockManager.isContainer(block) && lockManager.isLocked(block));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> lockManager.isContainer(block) && lockManager.isLocked(block));
    }

    private Block getBlockFromInventory(Inventory inventory) {
        if (inventory == null) return null;
        if (inventory.getLocation() != null && inventory.getLocation().getWorld() != null) {
            return inventory.getLocation().getBlock();
        }
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof BlockState state) {
            return state.getBlock();
        }
        if (holder instanceof DoubleChest doubleChest) {
            Location loc = doubleChest.getLocation();
            return loc != null ? loc.getBlock() : null;
        }
        return null;
    }
}

package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.locks.LockManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
            } else {
                // Owner or Admin breaking container -> unlock automatically
                lockManager.unlockContainer(block, player);
                player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Container unlocked.</gray>"));
            }
        }
    }
}

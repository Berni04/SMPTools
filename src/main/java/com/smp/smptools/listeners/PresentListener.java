package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.PresentManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PresentListener implements Listener {

    private final PresentManager presentManager;

    public PresentListener(PresentManager presentManager) {
        this.presentManager = presentManager;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.PLAYER_HEAD)
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        if (meta.getPersistentDataContainer().has(PresentManager.PRESENT_TIER_KEY, PersistentDataType.STRING)) {
            String tier = meta.getPersistentDataContainer().get(PresentManager.PRESENT_TIER_KEY,
                    PersistentDataType.STRING);
            presentManager.createPresent(event.getBlock().getLocation(), tier);
            event.getPlayer().sendMessage(SMPTools.getInstance().getMessageManager().getMessage("present.placed"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.PLAYER_HEAD)
            return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && player.isSneaking())
            return; // Allow creative players to inspect/break if sneaking

        if (presentManager.getPresentIdAt(block.getLocation()) != null) {
            event.setCancelled(true); // Prevent opening the head GUI if any
            presentManager.claimPresent(player, block.getLocation());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.PLAYER_HEAD)
            return;

        if (presentManager.getPresentIdAt(block.getLocation()) != null) {
            presentManager.removePresent(block.getLocation());
            event.getPlayer().sendMessage(SMPTools.getInstance().getMessageManager().getMessage("present.registry-removed"));
        }
    }
}

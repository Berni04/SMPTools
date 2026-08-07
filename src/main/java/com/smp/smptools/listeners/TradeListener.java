package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.trade.TradeManager;
import com.smp.smptools.trade.TradeSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

public class TradeListener implements Listener {

    private final SMPTools plugin;
    private final TradeManager tradeManager;

    public TradeListener(SMPTools plugin) {
        this.plugin = plugin;
        this.tradeManager = plugin.getTradeManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeSession session = tradeManager.getSession(player);
        if (session == null) return;

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR || event.getAction() == InventoryAction.CLONE_STACK) {
            event.setCancelled(true);
            return;
        }

        // Ensure click occurred inside trade inventory
        if (event.getClickedInventory() == null) return;

        boolean isTradeInv = event.getClickedInventory().equals(session.getInventory());
        boolean isP1 = player.equals(session.getPlayer1());

        int rawSlot = event.getRawSlot();

        if (isTradeInv) {
            // Divider, border, cancel, or ready buttons click logic
            if ((TradeSession.DIVIDER_SLOTS.contains(rawSlot) && rawSlot != TradeSession.CANCEL_SLOT) || (rawSlot >= 36 && rawSlot < 54 && rawSlot != TradeSession.P1_READY_SLOT && rawSlot != TradeSession.P2_READY_SLOT && rawSlot != TradeSession.CANCEL_SLOT)) {
                event.setCancelled(true);
                return;
            }

            if (rawSlot == TradeSession.CANCEL_SLOT) {
                event.setCancelled(true);
                session.cancelTrade(player);
                return;
            }

            if (rawSlot == TradeSession.P1_READY_SLOT) {
                event.setCancelled(true);
                if (isP1) session.toggleReady(player);
                return;
            }

            if (rawSlot == TradeSession.P2_READY_SLOT) {
                event.setCancelled(true);
                if (!isP1) session.toggleReady(player);
                return;
            }

            // Restrict slots: P1 can only modify P1_SLOTS, P2 can only modify P2_SLOTS
            if (isP1 && !TradeSession.P1_SLOTS.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            if (!isP1 && !TradeSession.P2_SLOTS.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            // Items changed in offer grid -> reset ready state
            session.resetReady();
        } else if (event.isShiftClick()) {
            // Shift clicking from player's inventory
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                List<Integer> targetSlots = isP1 ? TradeSession.P1_SLOTS_ORDERED : TradeSession.P2_SLOTS_ORDERED;
                if (transferToTradeSlots(session.getInventory(), clickedItem, targetSlots)) {
                    event.setCurrentItem(clickedItem.getAmount() > 0 ? clickedItem : null);
                    session.resetReady();
                }
            }
        }
    }

    private boolean transferToTradeSlots(Inventory tradeInv, ItemStack clickedItem, List<Integer> targetSlots) {
        int amountToMove = clickedItem.getAmount();
        int maxStack = clickedItem.getMaxStackSize();
        boolean movedAny = false;

        // 1st pass: merge into existing stacks of same item
        for (int slot : targetSlots) {
            if (amountToMove <= 0) break;
            ItemStack current = tradeInv.getItem(slot);
            if (current != null && current.isSimilar(clickedItem)) {
                int space = maxStack - current.getAmount();
                if (space > 0) {
                    int add = Math.min(space, amountToMove);
                    current.setAmount(current.getAmount() + add);
                    amountToMove -= add;
                    movedAny = true;
                }
            }
        }

        // 2nd pass: place in empty slots
        for (int slot : targetSlots) {
            if (amountToMove <= 0) break;
            ItemStack current = tradeInv.getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                int add = Math.min(maxStack, amountToMove);
                ItemStack newItem = clickedItem.clone();
                newItem.setAmount(add);
                tradeInv.setItem(slot, newItem);
                amountToMove -= add;
                movedAny = true;
            }
        }

        if (movedAny) {
            clickedItem.setAmount(amountToMove);
        }

        return movedAny;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeSession session = tradeManager.getSession(player);
        if (session == null) return;

        boolean isP1 = player.equals(session.getPlayer1());
        Set<Integer> allowedSlots = isP1 ? TradeSession.P1_SLOTS : TradeSession.P2_SLOTS;

        boolean touchedTradeInv = false;
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < session.getInventory().getSize()) {
                if (!allowedSlots.contains(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
                touchedTradeInv = true;
            }
        }

        if (touchedTradeInv) {
            session.resetReady();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        TradeSession session = tradeManager.getSession(player);
        if (session != null && event.getInventory().equals(session.getInventory()) && !session.isCompleted() && !session.isCancelled()) {
            session.cancelTrade(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        tradeManager.cleanupPendingRequests(player.getUniqueId());
        TradeSession session = tradeManager.getSession(player);
        if (session != null && !session.isCompleted() && !session.isCancelled()) {
            session.cancelTrade(player);
        }
    }
}

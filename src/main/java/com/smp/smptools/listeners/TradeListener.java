package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.trade.TradeManager;
import com.smp.smptools.trade.TradeSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

        // Ensure click occurred inside trade inventory
        if (event.getClickedInventory() == null) return;

        boolean isTradeInv = event.getClickedInventory().equals(session.getInventory());
        boolean isP1 = player.equals(session.getPlayer1());

        int rawSlot = event.getRawSlot();

        if (isTradeInv) {
            // Divider, border, cancel, or ready buttons click logic
            if (TradeSession.DIVIDER_SLOTS.contains(rawSlot) || (rawSlot >= 36 && rawSlot < 54 && rawSlot != TradeSession.P1_READY_SLOT && rawSlot != TradeSession.P2_READY_SLOT && rawSlot != TradeSession.CANCEL_SLOT)) {
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
            session.resetReady();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        TradeSession session = tradeManager.getSession(player);
        if (session != null && !session.isCompleted() && !session.isCancelled()) {
            session.cancelTrade(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TradeSession session = tradeManager.getSession(player);
        if (session != null && !session.isCompleted() && !session.isCancelled()) {
            session.cancelTrade(player);
        }
    }
}

package com.smp.smptools.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * Abstract base class for GUI listeners.
 * Provides common guard logic for inventory click events,
 * reducing boilerplate code in implementing classes.
 *
 * @author berni
 * @since 1.0-SNAPSHOT
 */
public abstract class AbstractGUIListener implements Listener {

    /**
     * Returns the title of the GUI this listener handles.
     * Used to identify if a click event is for this GUI.
     *
     * @return the GUI title string
     */
    protected abstract String getGuiTitle();

    /**
     * Checks if the given inventory view is for this GUI.
     *
     * @param view the inventory view to check
     * @return true if this is the GUI's inventory
     */
    protected boolean isGuiEvent(InventoryView view) {
        return view.getTitle().equals(getGuiTitle());
    }

    /**
     * Handles inventory click events for this GUI.
     * Cancels the event and delegates to onGuiClick() if valid.
     *
     * @param event the inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isGuiEvent(event.getView())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        onGuiClick(player, clicked, event.getSlot());
    }

    /**
     * Called when a player clicks on an item in this GUI.
     * Implementations should handle the click logic here.
     *
     * @param player the player who clicked
     * @param clicked the item that was clicked
     * @param slot the slot number that was clicked
     */
    protected abstract void onGuiClick(Player player, ItemStack clicked, int slot);
}

package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HomesGUIListener implements Listener {

    private final SMPTools plugin;

    public HomesGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Your Homes")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() != Material.NAME_TAG) {
            return;
        }

        String homeName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

        if (event.isLeftClick()) {
            // Teleport to home
            player.performCommand("home " + homeName);
            player.closeInventory();
        } else if (event.isRightClick()) {
            // Open delete confirmation
            openDeleteConfirmation(player, homeName);
        }
    }

    @EventHandler
    public void onDeleteConfirmationClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith("Delete home")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) {
            return;
        }

        String homeName = event.getView().getTitle().split("'")[1];

        if (clickedItem.getType() == Material.GREEN_WOOL) {
            // Confirm delete
            player.performCommand("delhome " + homeName);
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Home '" + homeName + "' has been deleted.");
        } else if (clickedItem.getType() == Material.RED_WOOL) {
            // Cancel delete
            player.closeInventory();
        }
    }

    private void openDeleteConfirmation(Player player, String homeName) {
        Inventory confirmationGUI = plugin.getServer().createInventory(null, 27, "Delete home '" + homeName + "'?");

        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        org.bukkit.inventory.meta.ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm");
        confirmItem.setItemMeta(confirmMeta);

        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        org.bukkit.inventory.meta.ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancelItem.setItemMeta(cancelMeta);

        confirmationGUI.setItem(11, confirmItem);
        confirmationGUI.setItem(15, cancelItem);

        player.openInventory(confirmationGUI);
    }
}

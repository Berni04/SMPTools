package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HomesGUIListener implements Listener {

    private final SMPTools plugin;

    public HomesGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("Your Homes"))) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() != Material.NAME_TAG) {
            return;
        }

        String homeName = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());

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
        String titlePlain = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!titlePlain.startsWith("Delete home '")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) {
            return;
        }

        String homeName = PlainTextComponentSerializer.plainText().serialize(event.getView().title())
                .replace("Delete home '", "").replace("'?", "");

        if (clickedItem.getType() == Material.GREEN_WOOL) {
            // Confirm delete
            player.performCommand("delhome " + homeName);
            player.closeInventory();
            player.sendMessage(Component.text("Home '" + homeName + "' has been deleted.", NamedTextColor.GREEN));
        } else if (clickedItem.getType() == Material.RED_WOOL) {
            // Cancel delete
            player.closeInventory();
        }
    }

    private void openDeleteConfirmation(Player player, String homeName) {
        Inventory confirmationGUI = plugin.getServer().createInventory(null, 27, Component.text("Delete home '" + homeName + "'?"));

        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(Component.text("Confirm", NamedTextColor.GREEN));
        confirmItem.setItemMeta(confirmMeta);

        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel", NamedTextColor.RED));
        cancelItem.setItemMeta(cancelMeta);

        confirmationGUI.setItem(11, confirmItem);
        confirmationGUI.setItem(15, cancelItem);

        player.openInventory(confirmationGUI);
    }
}

package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.InputValidator;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.Map;
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
    private final String homesGuiTitle;

    public HomesGUIListener(SMPTools plugin) {
        this.plugin = plugin;
        // Resolve the configurable title once so renaming the GUI does not
        // require code changes.
        this.homesGuiTitle = PlainTextComponentSerializer.plainText().serialize(
                plugin.getMessageManager().getMessage("homes.gui-title"));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof HomeDeleteConfirmHolder) {
            // Delete-confirmation GUIs are handled by the dedicated method
            // below; ignore them here to avoid double-handling.
            return;
        }
        String titlePlain = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!titlePlain.equals(homesGuiTitle)) {
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
        if (!(event.getInventory().getHolder() instanceof HomeDeleteConfirmHolder holder)) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) {
            return;
        }

        // Recover the home name from the holder rather than parsing it out
        // of the inventory title, so the configurable
        // homes.delete-confirm-title template can be renamed freely.
        String homeName = holder.getHomeName();

        if (clickedItem.getType() == Material.GREEN_WOOL) {
            player.performCommand("delhome " + homeName);
            player.closeInventory();
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage(
                    "homes.deleted-confirmation", player,
                    Map.of("name", InputValidator.sanitizeMiniMessage(homeName))));
        } else if (clickedItem.getType() == Material.RED_WOOL) {
            player.closeInventory();
        }
    }

    private void openDeleteConfirmation(Player player, String homeName) {
        HomeDeleteConfirmHolder holder = new HomeDeleteConfirmHolder(homeName);
        Inventory confirmationGUI = plugin.getServer().createInventory(holder, 27,
                plugin.getMessageManager().getMessage("homes.delete-confirm-title", player,
                        Map.of("name", InputValidator.sanitizeMiniMessage(homeName))));
        holder.setInventory(confirmationGUI);

        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(plugin.getMessageManager().getMessage("homes.confirm", player));
        confirmItem.setItemMeta(confirmMeta);

        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(plugin.getMessageManager().getMessage("homes.cancel", player));
        cancelItem.setItemMeta(cancelMeta);

        confirmationGUI.setItem(11, confirmItem);
        confirmationGUI.setItem(15, cancelItem);

        player.openInventory(confirmationGUI);
    }
}

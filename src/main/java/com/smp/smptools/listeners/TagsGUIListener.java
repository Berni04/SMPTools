package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TagsGUIListener implements Listener {

    private final SMPTools plugin;

    public TagsGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Your Titles")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) {
            return;
        }

        if (clickedItem.getType() == Material.NAME_TAG) {
            String title = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            plugin.getTagManager().setPlayerTitle(player, title);
            player.sendMessage(ChatColor.GREEN + "You have equipped the title: " + title);
            player.closeInventory();
        } else if (clickedItem.getType() == Material.BARRIER && clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "Clear Title")) {
            plugin.getTagManager().removePlayerTitle(player);
            player.sendMessage(ChatColor.GREEN + "Your title has been cleared.");
            player.closeInventory();
        }
    }
}

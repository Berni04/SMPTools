package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.ColorCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ColorGUIListener implements Listener {

    private final SMPTools plugin;

    public ColorGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Choose a Color")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String colorName = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            try {
                ChatColor chatColor = ChatColor.valueOf(colorName.toUpperCase());
                if (ColorCommand.colors.contains(chatColor)) {
                    plugin.getStatsConfig().set("stats." + player.getUniqueId().toString() + ".color", chatColor.name());
                    plugin.saveStatsConfig();
                    player.sendMessage(chatColor + "Your nametag color has been set to " + colorName);
                    player.closeInventory();

                    // Update display name immediately
                    String prefix = plugin.getStatsConfig().getString("stats." + player.getUniqueId().toString() + ".prefix", "");
                    String displayName = chatColor + "[" + prefix + "] " + player.getName();
                    player.setDisplayName(displayName);
                    player.setPlayerListName(displayName);
                }
            } catch (IllegalArgumentException e) {
                // Should not happen
            }
        }
    }
}

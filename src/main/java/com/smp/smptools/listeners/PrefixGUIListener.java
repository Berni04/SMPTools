package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.PrefixCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PrefixGUIListener implements Listener {

    private final SMPTools plugin;

    public PrefixGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Choose a Prefix")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                return;
            }

            Player player = (Player) event.getWhoClicked();
            String prefix = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            if (PrefixCommand.prefixes.contains(prefix)) {
                plugin.getStatsConfig().set("stats." + player.getUniqueId().toString() + ".prefix", prefix);
                plugin.saveStatsConfig();
                player.sendMessage(ChatColor.GREEN + "Your prefix has been set to " + prefix);
                player.closeInventory();

                // Update display name immediately
                String color = plugin.getStatsConfig().getString("stats." + player.getUniqueId().toString() + ".color", "WHITE");
                try {
                    ChatColor chatColor = ChatColor.valueOf(color.toUpperCase());
                    String displayName = chatColor + prefix + " " + player.getName();
                    player.setDisplayName(displayName);
                    player.setPlayerListName(displayName);
                } catch (IllegalArgumentException e) {
                    // Should not happen as we have a fallback
                }
            }
        }
    }
}

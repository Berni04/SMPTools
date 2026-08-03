package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Map;

public class InvseeGUIListener implements Listener {

    private final SMPTools plugin;

    public InvseeGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void openInvseeGUI(Player opener, Player target) {
        InvseeHolder holder = new InvseeHolder(target.getUniqueId());
        Component title = plugin.getMessageManager().getMessage("invsee.gui-title", null,
                Map.of("player", target.getName()));
        Inventory invseeGUI = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(invseeGUI);

        // Filler item
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);

        // Fill all slots with filler initially
        for (int i = 0; i < 54; i++) {
            invseeGUI.setItem(i, filler);
        }

        // --- Place Armor (Helmet, Chestplate, Leggings, Boots) ---
        ItemStack[] armorContents = target.getInventory().getArmorContents();
        if (armorContents.length >= 4) {
            invseeGUI.setItem(0, armorContents[3]); // Helmet
            invseeGUI.setItem(1, armorContents[2]); // Chestplate
            invseeGUI.setItem(2, armorContents[1]); // Leggings
            invseeGUI.setItem(3, armorContents[0]); // Boots
        }

        // --- Place Off-hand ---
        invseeGUI.setItem(8, target.getInventory().getItemInOffHand());

        // --- Place Main Inventory (27 slots) ---
        for (int i = 0; i < 27; i++) {
            invseeGUI.setItem(i + 18, target.getInventory().getItem(i + 9));
        }

        // --- Place Hotbar (9 slots) ---
        for (int i = 0; i < 9; i++) {
            invseeGUI.setItem(i + 45, target.getInventory().getItem(i));
        }

        // --- Add Labels for Armor and Off-hand (externalized) ---
        invseeGUI.setItem(9, createLabel(Material.LEATHER_HELMET, "invsee.label-helmet"));
        invseeGUI.setItem(10, createLabel(Material.LEATHER_CHESTPLATE, "invsee.label-chestplate"));
        invseeGUI.setItem(11, createLabel(Material.LEATHER_LEGGINGS, "invsee.label-leggings"));
        invseeGUI.setItem(12, createLabel(Material.LEATHER_BOOTS, "invsee.label-boots"));
        invseeGUI.setItem(17, createLabel(Material.SHIELD, "invsee.label-offhand"));

        opener.openInventory(invseeGUI);
    }

    private ItemStack createLabel(Material material, String messageKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getMessageManager().getMessage(messageKey));
        meta.lore(Collections.singletonList(
                plugin.getMessageManager().getMessage("invsee.armor-slot-lore")));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Primary check: our InventoryHolder. This is the only reliable
        // way to recognise an invsee GUI regardless of the configured
        // invsee.gui-title format.
        if (event.getInventory().getHolder() instanceof InvseeHolder) {
            event.setCancelled(true);
        }
    }
}

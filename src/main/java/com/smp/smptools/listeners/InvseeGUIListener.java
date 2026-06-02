package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

public class InvseeGUIListener implements Listener {

    private final SMPTools plugin;
    private static final String GUI_TITLE = "Invsee - ";

    public InvseeGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    public static void openInvseeGUI(Player opener, Player target) {
        // 54 slots: 4 armor, 1 off-hand, 27 inventory, 9 hotbar, plus filler
        Inventory invseeGUI = Bukkit.createInventory(null, 54, GUI_TITLE + target.getName());

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
        // PlayerInventory armor slots: 39 (Helmet), 38 (Chestplate), 37 (Leggings), 36 (Boots)
        // GUI slots: 0 (Helmet), 1 (Chestplate), 2 (Leggings), 3 (Boots)
        ItemStack[] armorContents = target.getInventory().getArmorContents();
        if (armorContents.length >= 4) {
            invseeGUI.setItem(0, armorContents[3]); // Helmet
            invseeGUI.setItem(1, armorContents[2]); // Chestplate
            invseeGUI.setItem(2, armorContents[1]); // Leggings
            invseeGUI.setItem(3, armorContents[0]); // Boots
        }

        // --- Place Off-hand ---
        // PlayerInventory off-hand slot: 40
        // GUI slot: 8
        invseeGUI.setItem(8, target.getInventory().getItemInOffHand());

        // --- Place Main Inventory (27 slots) ---
        // PlayerInventory slots: 9-35
        // GUI slots: 18-44 (3 rows of 9)
        for (int i = 0; i < 27; i++) {
            invseeGUI.setItem(i + 18, target.getInventory().getItem(i + 9));
        }

        // --- Place Hotbar (9 slots) ---
        // PlayerInventory slots: 0-8
        // GUI slots: 45-53 (last row)
        for (int i = 0; i < 9; i++) {
            invseeGUI.setItem(i + 45, target.getInventory().getItem(i));
        }

        // --- Add Labels for Armor and Off-hand ---
        invseeGUI.setItem(9, createLabel(Material.LEATHER_HELMET, "<gold>Helmet</gold>", SMPTools.getInstance()));
        invseeGUI.setItem(10, createLabel(Material.LEATHER_CHESTPLATE, "<gold>Chestplate</gold>", SMPTools.getInstance()));
        invseeGUI.setItem(11, createLabel(Material.LEATHER_LEGGINGS, "<gold>Leggings</gold>", SMPTools.getInstance()));
        invseeGUI.setItem(12, createLabel(Material.LEATHER_BOOTS, "<gold>Boots</gold>", SMPTools.getInstance()));
        invseeGUI.setItem(17, createLabel(Material.SHIELD, "<gold>Off-hand</gold>", SMPTools.getInstance())); // Label for off-hand

        opener.openInventory(invseeGUI);
    }

    private static ItemStack createLabel(Material material, String name, SMPTools plugin) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        meta.lore(Collections.singletonList(
                plugin.getMessageManager().getMessage("invsee.armor-slot-lore")));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Get the plain text title of the inventory
        String clickedTitle = event.getView().getTitle();
        
        if (!clickedTitle.startsWith(GUI_TITLE)) {
            return;
        }

        // Prevent any interaction with the inventory
        event.setCancelled(true);
    }
}

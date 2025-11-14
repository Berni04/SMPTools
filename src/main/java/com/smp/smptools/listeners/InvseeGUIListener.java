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
        // 54 slots: 4 armor, 36 inventory, 9 hotbar, 5 filler/info
        Inventory invseeGUI = Bukkit.createInventory(null, 54, GUI_TITLE + target.getName());

        // Fill with gray stained glass panes as filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            invseeGUI.setItem(i, filler);
        }

        // Place armor (slots 0-3 in Bukkit PlayerInventory, but we'll map them to specific GUI slots)
        // Helmet: 4, Chestplate: 13, Leggings: 22, Boots: 31
        ItemStack[] armorContents = target.getInventory().getArmorContents();
        if (armorContents.length >= 4) {
            invseeGUI.setItem(4, armorContents[3]); // Boots
            invseeGUI.setItem(13, armorContents[2]); // Leggings
            invseeGUI.setItem(22, armorContents[1]); // Chestplate
            invseeGUI.setItem(31, armorContents[0]); // Helmet
        }

        // Place main inventory (slots 9-35 in Bukkit PlayerInventory)
        // Map to GUI slots 36-53 (last two rows)
        for (int i = 0; i < 27; i++) { // Main inventory (27 slots)
            invseeGUI.setItem(i + 27, target.getInventory().getItem(i + 9));
        }

        // Place hotbar (slots 0-8 in Bukkit PlayerInventory)
        // Map to GUI slots 45-53 (last row)
        for (int i = 0; i < 9; i++) {
            invseeGUI.setItem(i + 45, target.getInventory().getItem(i));
        }

        // Add labels for armor slots
        invseeGUI.setItem(3, createLabel(Material.LEATHER_HELMET, "<gold>Helmet</gold>"));
        invseeGUI.setItem(12, createLabel(Material.LEATHER_CHESTPLATE, "<gold>Chestplate</gold>"));
        invseeGUI.setItem(21, createLabel(Material.LEATHER_LEGGINGS, "<gold>Leggings</gold>"));
        invseeGUI.setItem(30, createLabel(Material.LEATHER_BOOTS, "<gold>Boots</gold>"));

        opener.openInventory(invseeGUI);
    }

    private static ItemStack createLabel(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        meta.lore(Collections.singletonList(MiniMessage.miniMessage().deserialize("<gray>Armor Slot</gray>")));
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

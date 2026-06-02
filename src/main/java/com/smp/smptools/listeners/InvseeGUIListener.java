package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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

    private static SMPTools pluginInstance;
    private static String guiTitlePrefix;

    public InvseeGUIListener(SMPTools plugin) {
        pluginInstance = plugin;
        // Resolve the configurable title prefix once; the listener matches
        // against the plain text, so deriving the prefix from the template
        // allows admins to rename the GUI without code changes.
        String fullTitle = PlainTextComponentSerializer.plainText().serialize(
                plugin.getMessageManager().getMessage("invsee.gui-title", null,
                        Map.of("player", "")));
        guiTitlePrefix = fullTitle;
    }

    public static void openInvseeGUI(Player opener, Player target) {
        // 54 slots: 4 armor, 1 off-hand, 27 inventory, 9 hotbar, plus filler
        Component title = pluginInstance.getMessageManager().getMessage("invsee.gui-title", null,
                Map.of("player", target.getName()));
        Inventory invseeGUI = Bukkit.createInventory(null, 54, title);

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

    private static ItemStack createLabel(Material material, String messageKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(pluginInstance.getMessageManager().getMessage(messageKey));
        meta.lore(Collections.singletonList(
                pluginInstance.getMessageManager().getMessage("invsee.armor-slot-lore")));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String clickedTitle = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!clickedTitle.startsWith(guiTitlePrefix)) {
            return;
        }

        // Prevent any interaction with the inventory
        event.setCancelled(true);
    }
}

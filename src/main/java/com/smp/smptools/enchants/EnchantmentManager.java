package com.smp.smptools.enchants;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentManager {

    private final SMPTools plugin;

    public EnchantmentManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void applyEnchantment(ItemStack item, CustomEnchantment enchant) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, enchant.getKey());
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);

        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        lore.add(0, Component.text(enchant.getDisplayName(), NamedTextColor.GRAY)); // Add enchant name to lore
        meta.lore(lore);

        item.setItemMeta(meta);
    }

    public boolean hasEnchantment(ItemStack item, CustomEnchantment enchant) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, enchant.getKey());
        return meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
    }

    public boolean isApplicable(CustomEnchantment enchant, Material itemType) {
        List<String> applicableItems = plugin.getConfig().getStringList("features.custom-enchants." + enchant.getKey() + ".applicable-items");
        for (String applicable : applicableItems) {
            if (itemType.name().contains(applicable)) {
                return true;
            }
        }
        return false;
    }
}

package com.smp.smptools.missions;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RewardManager {

    public static final NamespacedKey ELYTRA_TRAIL_KEY = new NamespacedKey(SMPTools.getInstance(),
            "elytra_trail_color");

    public static void giveChromaticElytra(Player player, String color) {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();

        meta.displayName(Component.text("Chromatic Elytra", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("A legendary reward for a master of the skies.", NamedTextColor.GRAY));
        lore.add(Component.text(""));
        NamedTextColor trailColor;
        switch (color.toUpperCase()) {
            case "RED":
                trailColor = NamedTextColor.RED;
                break;
            case "BLUE":
                trailColor = NamedTextColor.BLUE;
                break;
            case "GREEN":
                trailColor = NamedTextColor.GREEN;
                break;
            case "PURPLE":
                trailColor = NamedTextColor.DARK_PURPLE;
                break;
            case "ORANGE":
                trailColor = NamedTextColor.GOLD;
                break;
            case "YELLOW":
                trailColor = NamedTextColor.YELLOW;
                break;
            case "BLACK":
                trailColor = NamedTextColor.BLACK;
                break;
            case "RAINBOW":
                trailColor = NamedTextColor.LIGHT_PURPLE;
                break; // Just for display
            default:
                trailColor = NamedTextColor.WHITE;
                break;
        }
        lore.add(Component.text("Trail Color: ", NamedTextColor.GRAY).append(Component.text(color, trailColor)));
        meta.lore(lore);

        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Store the color in the Persistent Data Container
        meta.getPersistentDataContainer().set(ELYTRA_TRAIL_KEY, PersistentDataType.STRING, color);

        elytra.setItemMeta(meta);

        player.getInventory().addItem(elytra);
        player.sendMessage(Component.text("You have received the Chromatic Elytra!", NamedTextColor.GREEN));
    }

    public static void giveReward(Player player, String reward) {
        if (reward.startsWith("item:")) {
            String[] parts = reward.substring(5).split(" ");
            Material material = Material.matchMaterial(parts[0]);
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

            if (material != null) {
                ItemStack item = new ItemStack(material, amount);
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(item);
                } else {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
                player.sendMessage(Component.text("Received " + amount + " " + material.name(), NamedTextColor.GREEN));
            }
        } else if (reward.startsWith("custom_item:")) {
            String customItem = reward.substring(12);
            if (customItem.equalsIgnoreCase("chromatic_elytra")) {
                // Handled by GUI for color selection, but if called directly (fallback)
                giveChromaticElytra(player, "WHITE");
            }
        } else if (reward.startsWith("command:")) {
            String command = reward.substring(8).replace("%player%", player.getName());
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
        } else {
            // Assume it's a command if no prefix (like "eco give")
            String command = reward.replace("%player%", player.getName());
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
        }
    }
}

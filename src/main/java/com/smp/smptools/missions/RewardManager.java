package com.smp.smptools.missions;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.CommandBlacklist;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardManager {

    public static final NamespacedKey ELYTRA_TRAIL_KEY = new NamespacedKey("smptools", "elytra_trail_color");

    public static void giveChromaticElytra(Player player, String color) {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();

        meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(SMPTools.getInstance().getMessageManager().getRawMessage("missions.chromatic-elytra-name")));

        List<Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(SMPTools.getInstance().getMessageManager().getRawMessage("missions.chromatic-elytra-lore-1")));
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
        // Build the trail color lore line using the static color component for the {color} part
        String trailLore = SMPTools.getInstance().getMessageManager().getRawMessage("missions.trail-color")
                .replace("{color}", "<" + trailColor.toString().replace("NamedTextColor", "").toLowerCase() + ">" + color);
        lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(trailLore));
        meta.lore(lore);

        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Store the color in the Persistent Data Container
        meta.getPersistentDataContainer().set(ELYTRA_TRAIL_KEY, PersistentDataType.STRING, color);

        elytra.setItemMeta(meta);

        player.getInventory().addItem(elytra);
        player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.chromatic-elytra-received", player));
    }

    public static void giveReward(Player player, String reward) {
        if (player == null || reward == null || reward.isBlank()) {
            return;
        }
        try {
            if (reward.startsWith("item:")) {
                String[] parts = reward.substring(5).trim().split("\\s+");
                if (parts.length == 0 || parts[0].isBlank()) {
                    if (SMPTools.getInstance() != null) {
                        SMPTools.getInstance().getLogger().warning("Empty item specification in mission reward: '" + reward + "'");
                    }
                    return;
                }
                Material material = Material.matchMaterial(parts[0]);
                int amount = 1;
                if (parts.length > 1) {
                    try {
                        amount = Math.max(1, Integer.parseInt(parts[1]));
                    } catch (NumberFormatException e) {
                        if (SMPTools.getInstance() != null) {
                            SMPTools.getInstance().getLogger().warning("Invalid item amount in mission reward '" + reward + "', defaulting to 1");
                        }
                        amount = 1;
                    }
                }

                if (material != null) {
                    ItemStack item = new ItemStack(material, amount);
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item);
                    } else {
                        player.getWorld().dropItem(player.getLocation(), item);
                    }
                    if (SMPTools.getInstance() != null && SMPTools.getInstance().getMessageManager() != null) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("amount", String.valueOf(amount));
                        placeholders.put("material", material.name());
                        player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.reward-received", player, placeholders));
                    }
                } else {
                    if (SMPTools.getInstance() != null) {
                        SMPTools.getInstance().getLogger().warning("Unknown material in mission reward: '" + parts[0] + "'");
                    }
                }
            } else if (reward.startsWith("custom_item:")) {
                String customItem = reward.substring(12);
                if (customItem.equalsIgnoreCase("chromatic_elytra")) {
                    // Handled by GUI for color selection, but if called directly (fallback)
                    giveChromaticElytra(player, "WHITE");
                }
            } else if (reward.startsWith("command:")) {
                String command = reward.substring(8).replace("%player%", player.getName());
                if (CommandBlacklist.isBlocked(command)) {
                    if (SMPTools.getInstance() != null) {
                        SMPTools.getInstance().getLogger().warning("Blocked dangerous command in mission reward: " + command);
                    }
                    return;
                }
                if (org.bukkit.Bukkit.getServer() != null) {
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
                }
            } else {
                String command = reward.replace("%player%", player.getName());
                if (CommandBlacklist.isBlocked(command)) {
                    if (SMPTools.getInstance() != null) {
                        SMPTools.getInstance().getLogger().warning("Blocked dangerous command in mission reward: " + command);
                    }
                    return;
                }
                if (org.bukkit.Bukkit.getServer() != null) {
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
                }
            }
        } catch (Exception e) {
            if (SMPTools.getInstance() != null) {
                SMPTools.getInstance().getLogger().warning("Failed to give mission reward '" + reward + "' to " + player.getName() + ": " + e.getMessage());
            }
        }
    }
}

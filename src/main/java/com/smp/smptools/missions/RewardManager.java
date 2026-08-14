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

    public static class ParsedItemReward {
        public final Material material;
        public final int amount;

        public ParsedItemReward(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }
    }

    public static ParsedItemReward parseItemReward(String reward) {
        if (reward == null || !reward.startsWith("item:")) return null;
        String[] parts = reward.substring(5).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) return null;
        Material material = Material.matchMaterial(parts[0]);
        if (material == null) return null;
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1]);
                if (amount <= 0) return null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new ParsedItemReward(material, amount);
    }

    public static boolean isValidReward(String reward) {
        if (reward == null || reward.isBlank()) return false;
        if (reward.startsWith("item:")) {
            return parseItemReward(reward) != null;
        } else if (reward.startsWith("custom_item:")) {
            String customItem = reward.substring(12).trim();
            if (customItem.equalsIgnoreCase("chromatic_elytra")) return true;
            return customItem.toLowerCase().startsWith("chromatic_elytra:");
        } else if (reward.startsWith("command:")) {
            String cmd = reward.substring(8);
            return !cmd.isBlank() && !CommandBlacklist.isBlocked(cmd);
        } else {
            return !reward.isBlank() && !CommandBlacklist.isBlocked(reward);
        }
    }

    public static boolean giveChromaticElytra(Player player, String color) {
        if (player == null) return false;
        try {
            ItemStack elytra = new ItemStack(Material.ELYTRA);
            ItemMeta meta = elytra.getItemMeta();

            if (SMPTools.getInstance() != null && SMPTools.getInstance().getMessageManager() != null) {
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
                        break;
                    default:
                        trailColor = NamedTextColor.WHITE;
                        break;
                }
                String trailLore = SMPTools.getInstance().getMessageManager().getRawMessage("missions.trail-color")
                        .replace("{color}", "<" + trailColor.toString().replace("NamedTextColor", "").toLowerCase() + ">" + color);
                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(trailLore));
                meta.lore(lore);
            }

            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            meta.getPersistentDataContainer().set(ELYTRA_TRAIL_KEY, PersistentDataType.STRING, color);
            elytra.setItemMeta(meta);

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(elytra);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            try {
                if (SMPTools.getInstance() != null && SMPTools.getInstance().getMessageManager() != null) {
                    player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.chromatic-elytra-received", player));
                }
            } catch (Exception ex) {
                if (SMPTools.getInstance() != null) {
                    SMPTools.getInstance().getLogger().warning("Failed to send chromatic elytra received message to " + player.getName() + ": " + ex.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            if (SMPTools.getInstance() != null) {
                SMPTools.getInstance().getLogger().warning("Failed to give chromatic elytra to " + player.getName() + ": " + e.getMessage());
            }
            return false;
        }
    }

    public static boolean giveReward(Player player, String reward) {
        if (player == null || reward == null || reward.isBlank()) {
            return false;
        }
        try {
            if (reward.startsWith("item:")) {
                ParsedItemReward parsed = parseItemReward(reward);
                if (parsed == null) {
                    if (SMPTools.getInstance() != null) {
                        SMPTools.getInstance().getLogger().warning("Invalid item specification in mission reward: '" + reward + "'");
                    }
                    return false;
                }

                ItemStack item = new ItemStack(parsed.material, parsed.amount);
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                if (SMPTools.getInstance() != null && SMPTools.getInstance().getMessageManager() != null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("amount", String.valueOf(parsed.amount));
                    placeholders.put("material", parsed.material.name());
                    player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("missions.reward-received", player, placeholders));
                }
                return true;
            } else if (reward.startsWith("custom_item:")) {
                String customItem = reward.substring(12).trim();
                if (customItem.equalsIgnoreCase("chromatic_elytra")) {
                    return giveChromaticElytra(player, "WHITE");
                } else if (customItem.toLowerCase().startsWith("chromatic_elytra:")) {
                    String color = customItem.substring(17).trim();
                    return giveChromaticElytra(player, color.isEmpty() ? "WHITE" : color);
                }
                return false;
            } else if (reward.startsWith("command:")) {
                String command = reward.substring(8).replace("%player%", player.getName());
                if (CommandBlacklist.isBlocked(command)) {
                    if (SMPTools.getInstance() != null) {
                        SMPTools.getInstance().getLogger().warning("Blocked dangerous command in mission reward: " + command);
                    }
                    return false;
                }
                if (org.bukkit.Bukkit.getServer() != null) {
                    return org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
                }
                return false;
            } else {
                String command = reward.replace("%player%", player.getName());
                if (CommandBlacklist.isBlocked(command)) {
                    if (SMPTools.getInstance() != null) {
                        SMPTools.getInstance().getLogger().warning("Blocked dangerous command in mission reward: " + command);
                    }
                    return false;
                }
                if (org.bukkit.Bukkit.getServer() != null) {
                    return org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
                }
                return false;
            }
        } catch (Exception e) {
            if (SMPTools.getInstance() != null) {
                SMPTools.getInstance().getLogger().warning("Failed to give mission reward '" + reward + "' to " + player.getName() + ": " + e.getMessage());
            }
            return false;
        }
    }
}

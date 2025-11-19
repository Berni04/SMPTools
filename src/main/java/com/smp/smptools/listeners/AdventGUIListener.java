package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.AdventManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdventGUIListener implements Listener {

    private final SMPTools plugin;
    private final AdventManager adventManager;
    private static final String GUI_TITLE = "Advent Calendar";

    public AdventGUIListener(SMPTools plugin, AdventManager adventManager) {
        this.plugin = plugin;
        this.adventManager = adventManager;
    }

    public void openAdventGUI(Player player) {
        // Optional: Check if it is December. For testing purposes, we might want to
        // allow it or have a config bypass.
        // But per requirements, it should check.
        if (!adventManager.isDecember() && !player.hasPermission("smptools.advent.bypass")) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red>The Advent Calendar is only available in December!</red>"));
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 45, Component.text(GUI_TITLE));
        int currentDay = adventManager.getCurrentDay();

        for (int day = 1; day <= 25; day++) {
            ItemStack item;
            if (adventManager.hasClaimed(player.getUniqueId(), day)) {
                item = createGuiItem(Material.MINECART, "<gold>Day " + day + "</gold>",
                        "<gray>Status: <green>Claimed</green></gray>");
            } else if (day <= currentDay || player.hasPermission("smptools.advent.bypass")) {
                item = createGuiItem(Material.LIME_STAINED_GLASS_PANE, "<green>Day " + day + "</green>",
                        "<gray>Status: <yellow>Available!</yellow></gray>", "<gray>Click to claim!</gray>");
            } else {
                item = createGuiItem(Material.RED_STAINED_GLASS_PANE, "<red>Day " + day + "</red>",
                        "<gray>Status: <red>Locked</red></gray>", "<gray>Come back later!</gray>");
            }
            gui.setItem(day - 1, item);
        }

        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(MiniMessage.miniMessage().deserialize(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text(GUI_TITLE))) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        int day = slot + 1;

        if (day < 1 || day > 25) {
            return;
        }

        int currentDay = adventManager.getCurrentDay();

        if (day > currentDay && !player.hasPermission("smptools.advent.bypass")) {
            player.sendMessage(MiniMessage.miniMessage()
                    .deserialize("<red>You cannot claim this reward yet! Come back on Dec " + day + ".</red>"));
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return;
        }

        if (adventManager.hasClaimed(player.getUniqueId(), day)) {
            player.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>You have already claimed this reward!</red>"));
            return;
        }

        // Claim reward
        adventManager.setClaimed(player.getUniqueId(), day);
        giveReward(player, day);

        player.sendMessage(
                MiniMessage.miniMessage().deserialize("<green>You claimed the reward for Day " + day + "!</green>"));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        // Refresh GUI
        openAdventGUI(player);
    }

    private void giveReward(Player player, int day) {
        List<String> rewards = adventManager.getRewards(day);
        for (String reward : rewards) {
            if (reward.startsWith("item:")) {
                try {
                    String[] parts = reward.substring(5).trim().split(" ");
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    player.getInventory().addItem(new ItemStack(material, amount));
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid item format in advent reward day " + day + ": " + reward);
                }
            } else if (reward.startsWith("broadcast:")) {
                String message = reward.substring(10).trim().replace("%player%", player.getName());
                Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
            } else {
                // Command
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("%player%", player.getName()));
            }
        }
    }
}

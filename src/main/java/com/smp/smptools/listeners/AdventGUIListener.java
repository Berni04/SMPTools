package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.AdventManager;
import com.smp.smptools.utils.HeadUtils;
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
                // Gold Present (Claimed)
                item = createGuiItem(HeadUtils.getCustomHead(
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDBkM2MzMDRmZjAxOGZjZTY1MDk5ZWFlZDlkNjhhNGM0OTAxNGFlNzA2MTc2ZjhlZTE4NzcyYTdiMzYyZjU4NSJ9fX0="),
                        "<gold>Day " + day + "</gold>", "<gray>Status: <green>Claimed</green></gray>");
            } else if (day <= currentDay || player.hasPermission("smptools.advent.bypass")) {
                // Green Present (Available)
                item = createGuiItem(HeadUtils.getCustomHead(
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWNmYWJiMzU1OTAzYzJiYzUzZjg4ODUyZDRkZjA5NjZjOTQ5OWI3NGMyNDczODk5ZGRkNWY3NzI3M2U2ODY4MSJ9fX0="),
                        "<green>Day " + day + "</green>", "<gray>Status: <yellow>Available!</yellow></gray>",
                        "<gray>Click to claim!</gray>");
            } else {
                // Red Present (Locked)
                item = createGuiItem(HeadUtils.getCustomHead(
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjVjOWQzMTc0M2Y0ZDM2YzVhODkxN2ExODE5ZjJmYWUwYWIwODM4MjJlNGZhYmYxNmU4ODkxMjgxMWU2NzZlMCJ9fX0="),
                        "<red>Day " + day + "</red>", "<gray>Status: <red>Locked</red></gray>",
                        "<gray>Come back later!</gray>");
            }
            gui.setItem(day - 1, item);
        }

        player.openInventory(gui);
    }

    private ItemStack createGuiItem(ItemStack item, String name, String... loreLines) {
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

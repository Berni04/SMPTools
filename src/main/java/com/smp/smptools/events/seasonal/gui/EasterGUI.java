package com.smp.smptools.events.seasonal.gui;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.seasonal.SeasonalManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 27-Slot Checklist GUI for the Easter Spring Egg Hunt (/easter).
 */
public class EasterGUI implements Listener {

    private static final Component TITLE = Component.text("🐣 Easter Egg Hunt Checklist", NamedTextColor.GREEN, TextDecoration.BOLD);

    private final SMPTools plugin;
    private final SeasonalManager seasonalManager;

    public EasterGUI(SMPTools plugin, SeasonalManager seasonalManager) {
        this.plugin = plugin;
        this.seasonalManager = seasonalManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        UUID uuid = player.getUniqueId();
        List<Integer> foundList = seasonalManager.getFoundEggs(uuid);
        int total = plugin.getSeasonalConfig().getInt("seasonal.easter.total_eggs", 15);

        // Fill background with black glass panes
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null, false);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Slots 0 to 14: Easter Egg cards
        int[] displaySlots = new int[]{
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14
        };

        for (int i = 0; i < total && i < displaySlots.length; i++) {
            int eggId = i + 1;
            boolean isFound = foundList.contains(eggId);

            if (isFound) {
                List<String> lore = List.of(
                        "<green>Status: DISCOVERED</green>",
                        "<gray>You collected this Easter Egg!</gray>"
                );
                gui.setItem(displaySlots[i], createItem(Material.EGG, "<green><b>🥚 Easter Egg #" + eggId + " [FOUND]</b></green>", lore, true));
            } else {
                String hint = seasonalManager.getEggHint(eggId);
                List<String> lore = List.of(
                        "<red>Status: MISSING</red>",
                        "<gray>Hint: " + hint + "</gray>",
                        "",
                        "<yellow>Find and right-click it in nature biomes!</yellow>"
                );
                gui.setItem(displaySlots[i], createItem(Material.PINK_STAINED_GLASS_PANE, "<red><b>🥚 Easter Egg #" + eggId + " [MISSING]</b></red>", lore, false));
            }
        }

        // Slot 22: Grand Reward Button
        boolean claimed = seasonalManager.hasClaimedEasterGrand(uuid);
        List<String> rewardLore = new ArrayList<>();
        rewardLore.add("<gray>Progress: <yellow>" + foundList.size() + "/" + total + " Found</yellow></gray>");
        rewardLore.add("");

        if (claimed) {
            rewardLore.add("<green>✔ Grand Reward Already Claimed!</green>");
            gui.setItem(22, createItem(Material.BARRIER, "<green><b>Grand Reward Claimed</b></green>", rewardLore, false));
        } else if (foundList.size() >= total) {
            rewardLore.add("<gold><b>🎉 Click to claim Chlorophyll Band & 12 Diamonds!</b></gold>");
            gui.setItem(22, createItem(Material.NETHER_STAR, "<gold><b>🏆 CLAIM GRAND REWARD!</b></gold>", rewardLore, true));
        } else {
            rewardLore.add("<red>Find all 15 hidden eggs to unlock the Grand Reward!</red>");
            gui.setItem(22, createItem(Material.CHEST, "<yellow><b>Grand Reward Locked (" + foundList.size() + "/" + total + ")</b></yellow>", rewardLore, false));
        }

        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore, boolean glint) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic>" + name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(MiniMessage.miniMessage().deserialize("<!italic>" + line));
                }
                meta.lore(loreList);
            }
            if (glint) {
                meta.setEnchantmentGlintOverride(true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(TITLE)) return;
        event.setCancelled(true);

        if (event.getRawSlot() == 22 && event.getWhoClicked() instanceof Player player) {
            boolean claimed = seasonalManager.claimEasterGrandReward(player);
            if (claimed) {
                openGUI(player);
            }
        }
    }
}

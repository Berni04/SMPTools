package com.smp.smptools.events.seasonal.gui;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.seasonal.SeasonType;
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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 27-Slot Central Seasonal Events Hub GUI.
 */
public class SeasonalGUI implements Listener {

    private static final Component TITLE = Component.text("🌟 Seasonal Events Hub", NamedTextColor.GOLD, TextDecoration.BOLD);

    private final SMPTools plugin;
    private final SeasonalManager seasonalManager;

    public SeasonalGUI(SMPTools plugin, SeasonalManager seasonalManager) {
        this.plugin = plugin;
        this.seasonalManager = seasonalManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        SeasonType active = seasonalManager.getCurrentSeason();

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Slot 4: Active Season Banner
        List<String> activeLore = new ArrayList<>();
        activeLore.add("<gray>Current Active Season: <green>" + active.getFormattedName() + "</green></gray>");
        activeLore.add("<gray>" + active.getDescription() + "</gray>");
        activeLore.add("");
        activeLore.add("<yellow>Click the seasonal cards below to open their events!</yellow>");
        gui.setItem(4, createItem(active.getGuiMaterial(), "<gold><b>Current Season: " + active.getDisplayName() + "</b></gold>", activeLore));

        // Slot 10: Halloween Card
        List<String> hwLore = List.of(
                "<gray>Dates: Oct 15 - Nov 2</gray>",
                "<gray>Features: Spooky Pumpkin Hunt, Headless Horseman</gray>",
                "",
                "<yellow>Click to open Halloween Checklist (/halloween)</yellow>"
        );
        gui.setItem(10, createItem(Material.JACK_O_LANTERN, "<gold><b>🎃 Halloween Spooky Fest</b></gold>", hwLore));

        // Slot 12: Easter Card
        List<String> easterLore = List.of(
                "<gray>Dates: Mar 25 - Apr 25</gray>",
                "<gray>Features: Hidden Egg Hunt, Golden Bunnies</gray>",
                "",
                "<yellow>Click to open Easter Checklist (/easter)</yellow>"
        );
        gui.setItem(12, createItem(Material.EGG, "<green><b>🐣 Easter Spring Fest</b></green>", easterLore));

        // Slot 14: Christmas Card
        boolean xmasEnabled = plugin.getConfig().getBoolean("features.christmas.enabled", true);
        List<String> xmasLore = xmasEnabled ? List.of(
                "<gray>Dates: Dec 1 - Jan 6</gray>",
                "<gray>Features: Advent Calendar, Secret Santa, Krampus</gray>",
                "",
                "<yellow>Click to open Advent Calendar (/advent)</yellow>"
        ) : List.of(
                "<gray>Dates: Dec 1 - Jan 6</gray>",
                "<red>Christmas features are currently disabled in config.</red>"
        );
        gui.setItem(14, createItem(xmasEnabled ? Material.SNOWBALL : Material.GRAY_DYE, "<aqua><b>🎄 Christmas & Winter Fest</b></aqua>", xmasLore));

        // Slot 16: Black Friday Card
        boolean bfEnabled = plugin.getConfig().getBoolean("features.blackfriday.enabled", true);
        boolean isBfEnabledInManager = plugin.getBlackFridayManager() != null && plugin.getBlackFridayManager().isEnabled();
        boolean isBfActive = bfEnabled && isBfEnabledInManager && seasonalManager.isSeasonActive(SeasonType.BLACK_FRIDAY);
        List<String> bfLore = (bfEnabled && isBfEnabledInManager) ? List.of(
                "<gray>Dates: Late November</gray>",
                "<gray>Features: 90% Villager Discounts</gray>",
                "<gray>Status: " + (isBfActive ? "<green>ACTIVE</green>" : "<red>INACTIVE</red>") + "</gray>",
                "",
                "<yellow>Click to view event details in chat</yellow>"
        ) : List.of(
                "<gray>Dates: Late November</gray>",
                "<red>Black Friday features are currently disabled in config.</red>"
        );
        gui.setItem(16, createItem((bfEnabled && isBfEnabledInManager) ? Material.EMERALD : Material.GRAY_DYE, "<green><b>🛍️ Black Friday Sale</b></green>", bfLore));

        player.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
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
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(TITLE)) return;
        event.setCancelled(true);

        if (event.getWhoClicked() instanceof Player player) {
            int slot = event.getRawSlot();
            if (slot == 10) {
                player.performCommand("halloween");
            } else if (slot == 12) {
                player.performCommand("easter");
            } else if (slot == 14) {
                if (plugin.getConfig().getBoolean("features.christmas.enabled", true)) {
                    player.performCommand("advent");
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Christmas features are disabled.</red>"));
                }
            } else if (slot == 16) {
                boolean bfEnabled = plugin.getConfig().getBoolean("features.blackfriday.enabled", true);
                boolean isBfEnabledInManager = plugin.getBlackFridayManager() != null && plugin.getBlackFridayManager().isEnabled();
                if (bfEnabled && isBfEnabledInManager) {
                    boolean active = seasonalManager.isSeasonActive(SeasonType.BLACK_FRIDAY);
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<green>🛍️ <b>Black Friday Event:</b> Villagers offer up to 90% trade discounts! Active: " +
                            (active ? "<green>YES</green>" : "<red>NO (Late November)</red>") + "</green>"
                    ));
                } else {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Black Friday features are disabled.</red>"));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().title().equals(TITLE)) {
            event.setCancelled(true);
        }
    }
}

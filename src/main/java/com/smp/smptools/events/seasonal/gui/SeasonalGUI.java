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
        List<String> xmasLore = List.of(
                "<gray>Dates: Dec 1 - Jan 6</gray>",
                "<gray>Features: Advent Calendar, Secret Santa, Krampus</gray>",
                "",
                "<yellow>Click to open Advent Calendar (/advent)</yellow>"
        );
        gui.setItem(14, createItem(Material.SNOWBALL, "<aqua><b>🎄 Christmas & Winter Fest</b></aqua>", xmasLore));

        // Slot 16: Black Friday Card
        List<String> bfLore = List.of(
                "<gray>Dates: Late November</gray>",
                "<gray>Features: 90% Villager Discounts</gray>",
                "",
                "<yellow>Use /blackfriday to view sales</yellow>"
        );
        gui.setItem(16, createItem(Material.EMERALD, "<green><b>🛍️ Black Friday Sale</b></green>", bfLore));

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
                player.performCommand("advent");
            } else if (slot == 16) {
                player.performCommand("blackfriday");
            }
        }
    }
}

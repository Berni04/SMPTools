package com.smp.smptools.artifacts.gui;

import com.smp.smptools.SMPTools;
import com.smp.smptools.artifacts.ArtifactManager;
import com.smp.smptools.artifacts.ArtifactType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 27-Slot Interactive GUI for equipping passive custom artifacts in the Artifact Pouch.
 */
public class ArtifactEquipmentGUI implements Listener {

    private static final Component TITLE = Component.text("🎒 Artifact Equipment Pouch", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
    private static final Set<Integer> ALLOWED_SLOTS = Set.of(10, 11, 12, 13, 14, 15, 16);

    private final SMPTools plugin;
    private final ArtifactManager artifactManager;

    public ArtifactEquipmentGUI(SMPTools plugin, ArtifactManager artifactManager) {
        this.plugin = plugin;
        this.artifactManager = artifactManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        Map<Integer, ItemStack> savedPouch = artifactManager.getEquippedPouch(player.getUniqueId());

        ItemStack filler = createItem(Material.PURPLE_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            if (!ALLOWED_SLOTS.contains(i)) {
                gui.setItem(i, filler);
            }
        }

        // Fill saved equipped items into allowed slots
        for (int slot : ALLOWED_SLOTS) {
            if (savedPouch.containsKey(slot)) {
                gui.setItem(slot, savedPouch.get(slot));
            }
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1.0f, 1.0f);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic>" + name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(TITLE)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < 27) {
            if (!ALLOWED_SLOTS.contains(rawSlot)) {
                event.setCancelled(true);
                return;
            }

            // Verify if placed item is a valid artifact
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                ArtifactType type = artifactManager.getArtifactType(cursor);
                if (type == null) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player p) {
                        p.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only Custom SMPTools Artifacts can be equipped in the Pouch!</red>"));
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().title().equals(TITLE)) return;

        if (event.getPlayer() instanceof Player player) {
            Inventory inv = event.getInventory();
            for (int slot : ALLOWED_SLOTS) {
                ItemStack item = inv.getItem(slot);
                artifactManager.setEquippedSlot(player.getUniqueId(), slot, item);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.2f);
        }
    }
}

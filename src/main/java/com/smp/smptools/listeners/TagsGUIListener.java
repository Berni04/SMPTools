package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TagsGUIListener implements Listener {

    private final SMPTools plugin;

    public TagsGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("Your Titles"))) {
            return;
        }

        event.setCancelled(true);

        if (plugin.getTagManager() == null) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null) {
            return;
        }

        if (clickedItem.getType() == Material.LIME_DYE && event.getSlot() < 54 && clickedItem.getItemMeta().hasDisplayName()) {
            String title = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());
            plugin.getTagManager().setPlayerTitle(player, title);
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("tags.equipped", player, Map.of("title", title)));
            player.closeInventory();
        } else if (clickedItem.getType() == Material.BARRIER && event.getSlot() == 53) {
            plugin.getTagManager().removePlayerTitle(player);
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("tags.cleared", player));
            player.closeInventory();
        }
    }
}

package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.PrefixCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PrefixGUIListener implements Listener {

    private final SMPTools plugin;

    public PrefixGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("Choose a Prefix"))) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() != Material.NAME_TAG) {
            return;
        }

        String prefix = PlainTextComponentSerializer.plainText().serialize(clickedItem.getItemMeta().displayName());

        if (PrefixCommand.prefixes.contains(prefix)) {
            plugin.getStatsConfig().set("players." + player.getUniqueId() + ".prefix", prefix);
            plugin.saveStatsConfig();
            plugin.getNameTagListener().updatePlayerName(player);
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("prefix.set", player, Map.of("prefix", prefix)));
            player.closeInventory();
        }
    }
}

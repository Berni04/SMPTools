package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.trails.TrailType;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TrailsGUIListener implements Listener {

    private final SMPTools plugin;

    public TrailsGUIListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isFlatFile = plugin.getStorageManager() != null &&
                plugin.getStorageManager().getProvider() instanceof com.smp.smptools.storage.FlatFileStorageProvider;
        if (isFlatFile) {
            if (plugin.getTrailManager().hasExplicitlySet(player)) return;
            plugin.getTrailManager().loadPlayerTrail(player);
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                if (!player.isOnline()) return;
                if (plugin.getTrailManager().hasExplicitlySet(player)) return;
                plugin.getTrailManager().loadPlayerTrail(player);
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTrailManager().removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.contains("Cosmetic Particle Trails")) return;

        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();
        if (slot == 22) {
            plugin.getTrailManager().setTrail(player, null);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            player.sendMessage(plugin.getMessageManager().getMessage("common.success", player, java.util.Map.of("message", "Cleared active particle trail.")));
            player.closeInventory();
            return;
        }

        int index = slot - 10;
        TrailType[] trails = TrailType.values();
        if (index >= 0 && index < trails.length) {
            TrailType selected = trails[index];
            boolean hasPerm = player.hasPermission(selected.getPermission()) || player.hasPermission("smptools.trails.all");
            if (!hasPerm) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
                return;
            }

            if (plugin.getTrailManager().getActiveTrail(player) == selected) {
                plugin.getTrailManager().setTrail(player, null);
                player.sendMessage(plugin.getMessageManager().getMessage("common.success", player, java.util.Map.of("message", "Unequipped " + selected.getDisplayName())));
            } else {
                plugin.getTrailManager().setTrail(player, selected);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.sendMessage(plugin.getMessageManager().getMessage("common.success", player, java.util.Map.of("message", "Equipped " + selected.getDisplayName())));
            }
            player.closeInventory();
        }
    }
}

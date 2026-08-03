package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.StatsCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsGUIListener implements Listener {

    private final StatsCommand statsCommand;
    private final SMPTools plugin;

    public StatsGUIListener(StatsCommand statsCommand) {
        this.statsCommand = statsCommand;
        this.plugin = statsCommand.getPlugin();
    }

    @EventHandler
    public void onStatsGUIClick(InventoryClickEvent event) {
        Component viewTitle = event.getView().title();
        String viewTitlePlain = PlainTextComponentSerializer.plainText().serialize(viewTitle);
        Player player = (Player) event.getWhoClicked();

        // Check if it's any of our stats GUIs
        if (viewTitlePlain.contains("'s Stats") || viewTitlePlain.contains("'s Deaths") || viewTitlePlain.contains("Death #")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String targetName = viewTitlePlain.split("'s|#")[0];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            // --- Navigation Logic ---

            // Main stats page -> Deaths List
            if (viewTitlePlain.endsWith("'s Stats") && clickedItem.getType() == Material.PAPER) {
                statsCommand.showDeathInfoGUI(player, target);
            }
            // Deaths List -> Detailed Death
            else if (viewTitlePlain.endsWith("'s Deaths") && clickedItem.getType() == Material.PAPER) {
                int deathIndex = getIndexFromItem(clickedItem, "Death #");
                if (deathIndex != -1) statsCommand.showDetailedDeathInfoGUI(player, target, deathIndex);
            }
            // Detailed Death -> Inventory View
            else if (viewTitlePlain.contains("'s Death #") && clickedItem.getType() == Material.CHEST) {
                int deathIndex = getIndexFromTitle(viewTitlePlain, "Death #");
                if (deathIndex != -1) statsCommand.showDeathInventoryGUI(player, target, deathIndex);
            }
            // --- Rollback Logic ---
            else if (viewTitlePlain.contains("Inventory") && clickedItem.getType() == Material.ANVIL) {
                if (!player.hasPermission("smptools.stats.rollback")) {
                    player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("stats.rollback-no-permission", player));
                    return;
                }

                int deathIndex = getIndexFromTitle(viewTitlePlain, "Death #");
                if (deathIndex != -1) {
                    rollbackInventory(player, target, deathIndex);
                }
            }
        }
    }

    private int getIndexFromItem(ItemStack item, String prefix) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return -1;
        try {
            String name = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
            return Integer.parseInt(name.replace(prefix, "").trim()) - 1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int getIndexFromTitle(String title, String prefix) {
        try {
            String number = title.split(prefix)[1].split(" ")[0];
            return Integer.parseInt(number.trim()) - 1;
        } catch (Exception e) {
            return -1;
        }
    }

    private void rollbackInventory(Player admin, OfflinePlayer target, int deathIndex) {
        String uuid = target.getUniqueId().toString();
        String path = "stats." + uuid + ".deaths_info";
        List<Map<?, ?>> deathInfo = plugin.getStatsConfig().getMapList(path);
        if (deathIndex < 0 || deathIndex >= deathInfo.size()) {
            return;
        }
        Map<String, Object> death = (Map<String, Object>) deathInfo.get(deathIndex);

        if (death.containsKey("rolled_back") && Boolean.TRUE.equals(death.get("rolled_back"))) {
            admin.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("stats.rollback-already-done", admin));
            return;
        }

        if (!target.isOnline()) {
            admin.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("stats.rollback-player-offline", admin));
            return;
        }

        Player targetPlayer = target.getPlayer();
        if (targetPlayer == null) {
            admin.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("stats.rollback-player-offline", admin));
            return;
        }

        List<?> inventory = (List<?>) death.get("inventory");

        if (inventory != null) {
            for (Object obj : inventory) {
                if (obj instanceof Map<?, ?> rawMap) {
                    Map<String, Object> itemMap = (Map<String, Object>) rawMap;
                    ItemStack item = null;
                    try {
                        item = ItemStack.deserialize(itemMap);
                    } catch (Exception e) {
                        Map<String, Object> copy = new HashMap<>(itemMap);
                        copy.putIfAbsent("==", "org.bukkit.inventory.ItemStack");
                        try {
                            item = ItemStack.deserialize(copy);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Failed to deserialize rollback item: " + itemMap);
                        }
                    }
                    if (item != null) {
                        for (ItemStack leftover : targetPlayer.getInventory().addItem(item).values()) {
                            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), leftover);
                        }
                    }
                }
            }
        }

        death.put("rolled_back", true);
        plugin.getStatsConfig().set(path, deathInfo);
        plugin.saveStatsConfig();

        admin.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("stats.rollback-success", admin, Map.of("index", String.valueOf(deathIndex + 1), "target", String.valueOf(target.getName()))));
        targetPlayer.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("stats.rollback-restored", targetPlayer));
        admin.closeInventory();
    }
}

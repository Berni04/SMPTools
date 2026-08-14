package com.smp.smptools.events.gui;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.EventManager;
import com.smp.smptools.events.minievents.MiniEventSession;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * 27-Slot GUI for displaying active event status, live top 5 leaderboard, and personal stats.
 */
public class EventGUI implements Listener {

    private static final Component TITLE = Component.text("🏆 Server Events Dashboard", NamedTextColor.GOLD, TextDecoration.BOLD);
    private final SMPTools plugin;
    private final EventManager eventManager;
    private final Set<UUID> openViewers = new HashSet<>();
    private BukkitTask refreshTask = null;

    public EventGUI(SMPTools plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void ensureRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) return;

        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (openViewers.isEmpty()) {
                if (refreshTask != null) {
                    refreshTask.cancel();
                    refreshTask = null;
                }
                return;
            }

            Iterator<UUID> it = openViewers.iterator();
            while (it.hasNext()) {
                UUID uuid = it.next();
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline() || !player.getOpenInventory().title().equals(TITLE)) {
                    it.remove();
                } else {
                    updateDynamicSlots(player, player.getOpenInventory().getTopInventory());
                }
            }

            if (openViewers.isEmpty() && refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
        }, 20L, 20L);
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        setupInitialInventory(gui);
        updateDynamicSlots(player, gui);
        player.openInventory(gui);
        openViewers.add(player.getUniqueId());
        ensureRefreshTask();
    }

    private void setupInitialInventory(Inventory gui) {
        // Fill background with black glass panes
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, filler);
        }

        // Slot 15: Static Rules / Info Card
        List<String> statsLore = new ArrayList<>();
        statsLore.add("<gray>View live active mini-event status.</gray>");
        statsLore.add("<gray>Updates automatically every second while open.</gray>");
        ItemStack infoItem = createItem(Material.BOOK, "<cyan><b>Event Info & Rules</b></cyan>", statsLore);
        gui.setItem(15, infoItem);
    }

    private void updateDynamicSlots(Player player, Inventory gui) {
        MiniEventSession active = eventManager.getActiveSession();

        // Slot 11: Active Event Card
        if (active != null && active.isActive()) {
            List<String> activeLore = new ArrayList<>();
            activeLore.add("<gray>Status: <green>ACTIVE</green></gray>");
            activeLore.add("<gray>Time Remaining: <yellow>" + active.formatTime(active.getRemainingSeconds()) + "</yellow></gray>");
            activeLore.add("<gray>Description: " + active.getType().getDescription() + "</gray>");
            activeLore.add("");
            activeLore.add("<yellow>Your Points: <gold>" + active.getPlayerScore(player.getUniqueId()) + " pts</gold></yellow>");

            ItemStack activeItem = createItem(active.getType().getGuiMaterial(), active.getType().getFormattedName(), activeLore);
            gui.setItem(11, activeItem);

            // Slot 13: Live Top 5 Leaderboard
            List<String> topLore = new ArrayList<>();
            List<Map.Entry<UUID, Integer>> top5 = active.getTopPlayers(5);
            if (top5.isEmpty()) {
                topLore.add("<gray>No points scored yet.</gray>");
            } else {
                for (int i = 0; i < top5.size(); i++) {
                    Map.Entry<UUID, Integer> entry = top5.get(i);
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name == null) name = "Unknown";
                    topLore.add("<gold>#" + (i + 1) + "</gold> <yellow>" + name + "</yellow>: <green>" + entry.getValue() + " pts</green>");
                }
            }
            ItemStack leaderboardItem = createItem(Material.NETHER_STAR, "<gold><b>Live Top 5 Leaderboard</b></gold>", topLore);
            gui.setItem(13, leaderboardItem);

        } else {
            List<String> inactiveLore = new ArrayList<>();
            inactiveLore.add("<gray>Status: <red>INACTIVE</red></gray>");
            inactiveLore.add("<gray>No mini-event is currently running.</gray>");
            inactiveLore.add("<gray>Automated timer will start the next event soon!</gray>");
            ItemStack inactiveItem = createItem(Material.CLOCK, "<red><b>No Active Event</b></red>", inactiveLore);
            gui.setItem(11, inactiveItem);

            ItemStack leaderboardItem = createItem(Material.BARRIER, "<gray><b>Leaderboard Unavailable</b></gray>", List.of("<gray>Wait for an event to start.</gray>"));
            gui.setItem(13, leaderboardItem);
        }
    }

    private ItemStack createItem(Material mat, String displayNameMiniMsg, List<String> loreMiniMsg) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<!italic>" + displayNameMiniMsg));
            if (loreMiniMsg != null && !loreMiniMsg.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreMiniMsg) {
                    lore.add(MiniMessage.miniMessage().deserialize("<!italic>" + line));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().title().equals(TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().title().equals(TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().title().equals(TITLE)) {
            openViewers.remove(event.getPlayer().getUniqueId());
            if (openViewers.isEmpty() && refreshTask != null) {
                refreshTask.cancel();
                refreshTask = null;
            }
        }
    }
}

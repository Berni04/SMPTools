package com.smp.smptools.trade;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TradeSession {

    private final SMPTools plugin;
    private final Player player1;
    private final Player player2;
    private final Inventory inventory;

    private boolean player1Ready = false;
    private boolean player2Ready = false;
    private boolean completed = false;
    private boolean cancelled = false;

    // Slot definitions
    public static final Set<Integer> P1_SLOTS = Set.of(0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30);
    public static final Set<Integer> P2_SLOTS = Set.of(5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35);
    public static final List<Integer> P1_SLOTS_ORDERED = List.of(0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30);
    public static final List<Integer> P2_SLOTS_ORDERED = List.of(5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35);
    public static final Set<Integer> DIVIDER_SLOTS = Set.of(4, 13, 22, 31, 40, 49);
    
    public static final int P1_READY_SLOT = 45;
    public static final int P2_READY_SLOT = 53;
    public static final int CANCEL_SLOT = 49;

    private org.bukkit.scheduler.BukkitTask timeoutTask;

    public TradeSession(SMPTools plugin, Player player1, Player player2) {
        this.plugin = plugin;
        this.player1 = player1;
        this.player2 = player2;

        Component title = MiniMessage.miniMessage().deserialize(
                "<blue>" + player1.getName() + "</blue> <gray>⇄</gray> <green>" + player2.getName() + "</green>"
        );
        this.inventory = Bukkit.createInventory(null, 54, title);
        setupInitialUI();
    }

    private void setupInitialUI() {
        // Divider glass
        ItemStack divider = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : DIVIDER_SLOTS) {
            inventory.setItem(slot, divider);
        }

        // Fill non-usable bottom row glass
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 36; slot < 54; slot++) {
            if (slot != P1_READY_SLOT && slot != P2_READY_SLOT && slot != CANCEL_SLOT && !DIVIDER_SLOTS.contains(slot)) {
                inventory.setItem(slot, border);
            }
        }

        updateButtons();
    }

    public void updateButtons() {
        // P1 Ready Button (Slot 45)
        Material p1Mat = player1Ready ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String p1Text = player1Ready ? "<green>✔ " + player1.getName() + " is READY</green>" : "<red>✖ " + player1.getName() + " NOT READY</red>";
        inventory.setItem(P1_READY_SLOT, createGuiItem(p1Mat, p1Text, "<gray>Click to toggle ready status</gray>"));

        // P2 Ready Button (Slot 53)
        Material p2Mat = player2Ready ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String p2Text = player2Ready ? "<green>✔ " + player2.getName() + " is READY</green>" : "<red>✖ " + player2.getName() + " NOT READY</red>";
        inventory.setItem(P2_READY_SLOT, createGuiItem(p2Mat, p2Text, "<gray>Click to toggle ready status</gray>"));

        // Cancel Button (Slot 49)
        inventory.setItem(CANCEL_SLOT, createGuiItem(Material.BARRIER, "<red>Cancel Trade</red>", "<gray>Click to abort trade</gray>"));
    }

    public void open() {
        player1.openInventory(inventory);
        player2.openInventory(inventory);

        this.timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!completed && !cancelled) {
                cancelTrade("Trade timed out after 60 seconds.");
            }
        }, 60 * 20L);
    }

    private void cancelTask() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }

    public void toggleReady(Player player) {
        if (player.equals(player1)) {
            player1Ready = !player1Ready;
        } else if (player.equals(player2)) {
            player2Ready = !player2Ready;
        }
        updateButtons();

        if (player1Ready && player2Ready) {
            completeTrade();
        }
    }

    public void resetReady() {
        this.player1Ready = false;
        this.player2Ready = false;
        updateButtons();
    }

    public synchronized void completeTrade() {
        if (completed || cancelled) return;
        completed = true;
        cancelTask();

        List<ItemStack> p1Items = getOfferedItems(P1_SLOTS);
        List<ItemStack> p2Items = getOfferedItems(P2_SLOTS);

        // Clear trade slots before closing
        for (int slot : P1_SLOTS) inventory.setItem(slot, null);
        for (int slot : P2_SLOTS) inventory.setItem(slot, null);

        // Give P1 items to P2
        for (ItemStack item : p1Items) {
            giveOrDrop(player2, item);
        }

        // Give P2 items to P1
        for (ItemStack item : p2Items) {
            giveOrDrop(player1, item);
        }

        player1.playSound(player1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player2.playSound(player2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        player1.sendMessage(plugin.getMessageManager().getMessage("trade.completed"));
        player2.sendMessage(plugin.getMessageManager().getMessage("trade.completed"));

        plugin.getTradeManager().removeSession(player1.getUniqueId(), player2.getUniqueId());

        player1.closeInventory();
        player2.closeInventory();
    }

    public synchronized void cancelTrade(String reason) {
        if (completed || cancelled) return;
        cancelled = true;
        cancelTask();

        List<ItemStack> p1Items = getOfferedItems(P1_SLOTS);
        List<ItemStack> p2Items = getOfferedItems(P2_SLOTS);

        // Clear trade slots
        for (int slot : P1_SLOTS) inventory.setItem(slot, null);
        for (int slot : P2_SLOTS) inventory.setItem(slot, null);

        // Return items
        for (ItemStack item : p1Items) giveOrDrop(player1, item);
        for (ItemStack item : p2Items) giveOrDrop(player2, item);

        if (reason != null && !reason.isEmpty()) {
            Component msg = MiniMessage.miniMessage().deserialize("<red>Trade cancelled: " + reason + "</red>");
            player1.sendMessage(msg);
            player2.sendMessage(msg);
        } else {
            player1.sendMessage(plugin.getMessageManager().getMessage("trade.cancelled"));
            player2.sendMessage(plugin.getMessageManager().getMessage("trade.cancelled"));
        }

        plugin.getTradeManager().removeSession(player1.getUniqueId(), player2.getUniqueId());

        player1.closeInventory();
        player2.closeInventory();
    }

    public synchronized void cancelTrade(Player initiator) {
        cancelTrade((String) null);
    }

    private List<ItemStack> getOfferedItems(Set<Integer> slots) {
        List<ItemStack> list = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                list.add(item.clone());
            }
        }
        return list;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        var remaining = player.getInventory().addItem(item);
        for (ItemStack rem : remaining.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rem);
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(name));
            if (lore.length > 0) {
                List<Component> loreComponents = new ArrayList<>();
                for (String l : lore) {
                    loreComponents.add(MiniMessage.miniMessage().deserialize(l));
                }
                meta.lore(loreComponents);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public Inventory getInventory() { return inventory; }
    public boolean isCompleted() { return completed; }
    public boolean isCancelled() { return cancelled; }
}

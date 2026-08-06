package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.bounty.Bounty;
import com.smp.smptools.bounty.BountyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BountyGUIListener implements Listener {

    private final SMPTools plugin;
    private final BountyManager bountyManager;

    // Track active place GUI sessions: placer UUID -> target UUID
    private final Map<UUID, UUID> activePlaceSessions = new ConcurrentHashMap<>();
    // Track target being viewed in details GUI: viewer UUID -> target UUID
    private final Map<UUID, UUID> detailsViewMap = new ConcurrentHashMap<>();
    // Track claim GUI items mapping: viewer UUID -> Map<slot, Bounty>
    private final Map<UUID, Map<Integer, Bounty>> claimMap = new ConcurrentHashMap<>();

    public BountyGUIListener(SMPTools plugin) {
        this.plugin = plugin;
        this.bountyManager = plugin.getBountyManager();
    }

    public void openPlaceGUI(Player placer, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, MiniMessage.miniMessage().deserialize("<gold>Deposit Bounty: " + target.getName() + "</gold>"));
        activePlaceSessions.put(placer.getUniqueId(), target.getUniqueId());

        // Fill row 3 control border
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            border.setItemMeta(meta);
        }
        for (int i = 18; i < 27; i++) {
            if (i != 22 && i != 26) inv.setItem(i, border);
        }

        // Confirm button (Slot 22)
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(MiniMessage.miniMessage().deserialize("<green><b>CONFIRM BOUNTY</b></green>"));
            confirmMeta.lore(List.of(MiniMessage.miniMessage().deserialize("<gray>Click to deposit items as bounty</gray>")));
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(22, confirm);

        // Cancel button (Slot 26)
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(MiniMessage.miniMessage().deserialize("<red><b>CANCEL</b></red>"));
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(26, cancel);

        placer.openInventory(inv);
    }

    public void openBountyListGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize("<gold>Active Player Bounties</gold>"));
        Map<UUID, List<Bounty>> grouped = bountyManager.getActiveBountiesGroupedByTarget();

        int slot = 0;
        for (Map.Entry<UUID, List<Bounty>> entry : grouped.entrySet()) {
            if (slot >= 54) break;
            UUID targetUuid = entry.getKey();
            List<Bounty> list = entry.getValue();

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                String name = target.getName() != null ? target.getName() : "Unknown";
                meta.displayName(MiniMessage.miniMessage().deserialize("<red><b>" + name + "</b></red>"));

                int totalItems = 0;
                for (Bounty b : list) totalItems += b.getItems().size();

                meta.lore(List.of(
                        MiniMessage.miniMessage().deserialize("<gray>Active Bounties: </gray><yellow>" + list.size() + "</yellow>"),
                        MiniMessage.miniMessage().deserialize("<gray>Total Offered Items: </gray><yellow>" + totalItems + "</yellow>"),
                        MiniMessage.miniMessage().deserialize("<green>Click to view bounty items</green>")
                ));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        player.openInventory(inv);
    }

    public void openBountyDetailsGUI(Player player, UUID targetUuid) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String targetName = target.getName() != null ? target.getName() : "Unknown";
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize("<gold>Bounty on " + targetName + "</gold>"));
        detailsViewMap.put(player.getUniqueId(), targetUuid);

        List<Bounty> active = bountyManager.getActiveBountiesForTarget(targetUuid);
        int slot = 0;
        for (Bounty b : active) {
            for (ItemStack item : b.getItems()) {
                if (slot >= 45) break;
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack copy = item.clone();
                    ItemMeta meta = copy.getItemMeta();
                    if (meta != null) {
                        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                        lore.add(Component.empty());
                        lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>Placed by: " + b.getPlacerName() + "</dark_gray>"));
                        meta.lore(lore);
                        copy.setItemMeta(meta);
                    }
                    inv.setItem(slot++, copy);
                }
            }
        }

        // Back button (Slot 49)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(MiniMessage.miniMessage().deserialize("<yellow>← Back to Bounties List</yellow>"));
            back.setItemMeta(backMeta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    public void openClaimGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MiniMessage.miniMessage().deserialize("<gold>Claim Bounties & Refunds</gold>"));
        List<Bounty> claimable = bountyManager.getClaimableBountiesForPlayer(player);

        Map<Integer, Bounty> slotToBounty = new HashMap<>();
        int slot = 0;

        for (Bounty b : claimable) {
            if (slot >= 54) break;
            boolean isKiller = b.isClaimableByKiller(player.getUniqueId());
            boolean isRefund = b.isRefundableToPlacer(player.getUniqueId());

            ItemStack icon = new ItemStack(b.getItems().isEmpty() ? Material.CHEST : b.getItems().get(0).getType());
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                if (isKiller) {
                    meta.displayName(MiniMessage.miniMessage().deserialize("<green><b>CLAIM BOUNTY: " + b.getTargetName() + "</b></green>"));
                } else if (isRefund) {
                    meta.displayName(MiniMessage.miniMessage().deserialize("<yellow><b>REFUND EXPIRED BOUNTY: " + b.getTargetName() + "</b></yellow>"));
                }

                List<Component> lore = new ArrayList<>();
                lore.add(MiniMessage.miniMessage().deserialize("<gray>Items: </gray><yellow>" + b.getItems().size() + " items</yellow>"));
                if (isKiller) {
                    long remainingMs = (b.getKilledTimestamp() + Bounty.SEVEN_DAYS_MS) - System.currentTimeMillis();
                    long days = remainingMs / (24 * 60 * 60 * 1000L);
                    long hours = (remainingMs % (24 * 60 * 60 * 1000L)) / (60 * 60 * 1000L);
                    lore.add(MiniMessage.miniMessage().deserialize("<gray>Expires in: </gray><red>" + days + "d " + hours + "h</red>"));
                    lore.add(MiniMessage.miniMessage().deserialize("<green>Click to claim all items into inventory!</green>"));
                } else if (isRefund) {
                    lore.add(MiniMessage.miniMessage().deserialize("<red>Killer did not claim within 7 days!</red>"));
                    lore.add(MiniMessage.miniMessage().deserialize("<yellow>Click to refund your deposited items!</yellow>"));
                }
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(slot, icon);
            slotToBounty.put(slot, b);
            slot++;
        }

        claimMap.put(player.getUniqueId(), slotToBounty);
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // 1. Placing GUI
        if (title.startsWith("Deposit Bounty: ")) {
            int slot = event.getRawSlot();
            if (slot >= 18 && slot < 27) {
                event.setCancelled(true);

                if (slot == 26) {
                    // Cancel
                    player.closeInventory();
                    return;
                }

                if (slot == 22) {
                    // Confirm
                    Inventory inv = event.getClickedInventory();
                    if (inv == null) return;

                    List<ItemStack> items = new ArrayList<>();
                    for (int i = 0; i < 18; i++) {
                        ItemStack is = inv.getItem(i);
                        if (is != null && is.getType() != Material.AIR) {
                            items.add(is.clone());
                        }
                    }

                    if (items.isEmpty()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must deposit at least 1 item for the bounty!</red>"));
                        return;
                    }

                    UUID targetUuid = activePlaceSessions.get(player.getUniqueId());
                    Player target = targetUuid != null ? Bukkit.getPlayer(targetUuid) : null;

                    if (target == null || !target.isOnline()) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Target player is no longer online or session is invalid!</red>"));
                        returnItemsFromGUI(player, inv);
                        activePlaceSessions.remove(player.getUniqueId());
                        player.closeInventory();
                        return;
                    }

                    boolean success = bountyManager.createBounty(player, target, items);
                    if (success) {
                        // Clear items from GUI only after creation succeeds
                        for (int i = 0; i < 18; i++) {
                            inv.setItem(i, null);
                        }
                        activePlaceSessions.remove(player.getUniqueId());

                        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(
                                "<gold>🎯 " + player.getName() + "</gold> <gray>placed a bounty on</gray> <red>" + target.getName() + "</red>!"
                        ));
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        player.closeInventory();
                    } else {
                        returnItemsFromGUI(player, inv);
                        activePlaceSessions.remove(player.getUniqueId());
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to create bounty. Items have been returned to your inventory.</red>"));
                        player.closeInventory();
                    }
                }
            }
            return;
        }

        // 2. Bounty List GUI
        if (title.equals("Active Player Bounties")) {
            event.setCancelled(true);
            ItemStack current = event.getCurrentItem();
            if (current != null && current.getType() == Material.PLAYER_HEAD && current.getItemMeta() instanceof SkullMeta meta) {
                OfflinePlayer target = meta.getOwningPlayer();
                if (target != null) {
                    openBountyDetailsGUI(player, target.getUniqueId());
                }
            }
            return;
        }

        // 3. Bounty Details GUI
        if (title.startsWith("Bounty on ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 49) {
                openBountyListGUI(player);
            }
            return;
        }

        // 4. Claim GUI
        if (title.equals("Claim Bounties & Refunds")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Map<Integer, Bounty> map = claimMap.get(player.getUniqueId());
            if (map != null && map.containsKey(slot)) {
                Bounty bounty = map.get(slot);
                if (bountyManager.claimOrRefundBounty(bounty, player)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Bounty items claimed successfully!</green>"));
                    openClaimGUI(player); // refresh GUI
                }
            }
        }
    }

    private void returnItemsFromGUI(Player player, Inventory inv) {
        if (inv == null) return;
        for (int i = 0; i < 18; i++) {
            ItemStack is = inv.getItem(i);
            if (is != null && is.getType() != Material.AIR) {
                inv.setItem(i, null);
                var remaining = player.getInventory().addItem(is.clone());
                for (ItemStack rem : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rem);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.startsWith("Deposit Bounty: ")) {
            UUID targetUuid = activePlaceSessions.remove(player.getUniqueId());
            if (targetUuid != null) {
                returnItemsFromGUI(player, event.getInventory());
            }
        }

        detailsViewMap.remove(player.getUniqueId());
        claimMap.remove(player.getUniqueId());
    }
}

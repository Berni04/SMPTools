package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.SecretSantaManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SecretSantaCommand extends AbstractPlayerCommand implements Listener {

    private final SecretSantaManager manager;

    public SecretSantaCommand(SMPTools plugin, SecretSantaManager manager) {
        super(plugin);
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        SecretSantaManager.Phase phase = manager.getCurrentPhase();

        switch (sub) {
            case "join":
                if (phase != SecretSantaManager.Phase.REGISTRATION) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.registration-closed"));
                    return true;
                }
                if (manager.isRegistered(player.getUniqueId())) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.already-registered"));
                    return true;
                }
                manager.registerPlayer(player.getUniqueId());
                player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.registered"));
                break;

            case "target":
                if (phase == SecretSantaManager.Phase.REGISTRATION) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.registration-closed"));
                    return true;
                }
                UUID targetUUID = manager.getTarget(player.getUniqueId());
                if (targetUUID == null) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.not-registered"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
                player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.assignment", player,
                        Map.of("target", String.valueOf(target.getName()))));
                break;

            case "deposit":
                if (phase != SecretSantaManager.Phase.PREPARATION) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.registration-closed"));
                    return true;
                }
                UUID depositTarget = manager.getTarget(player.getUniqueId());
                if (depositTarget == null) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.not-registered"));
                    return true;
                }
                if (manager.hasGiftDeposited(depositTarget)) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.already-registered"));
                    return true;
                }
                openDepositGUI(player, depositTarget);
                break;

            case "claim":
                if (phase != SecretSantaManager.Phase.CELEBRATION) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.registration-closed"));
                    return true;
                }
                ItemStack[] gift = manager.getGift(player.getUniqueId());
                if (gift == null || gift.length == 0) {
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.not-registered"));
                    return true;
                }
                for (ItemStack item : gift) {
                    if (item != null && item.getType() != Material.AIR) {
                        HashMap<Integer, ItemStack> left = player.getInventory().addItem(item);
                        for (ItemStack drop : left.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
                player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.gift-deposited"));
                break;

            case "admin":
                if (!player.hasPermission("smptools.admin")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("start")) {
                    manager.generateMatches();
                    player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.matches-generated"));
                }
                break;

            default:
                sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.help-header", player));
        player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.help-join", player));
        player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.help-target", player));
        player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.help-deposit", player));
        player.sendMessage(plugin.getMessageManager().getMessage("secret-santa.help-claim", player));
    }

    private void openDepositGUI(Player player, UUID targetUUID) {
        com.smp.smptools.christmas.SecretSantaHolder holder = new com.smp.smptools.christmas.SecretSantaHolder(targetUUID);
        Inventory gui = Bukkit.createInventory(holder, 27,
                plugin.getMessageManager().getMessage("secret-santa.deposit-gui-title", player,
                        Map.of("target", String.valueOf(Bukkit.getOfflinePlayer(targetUUID).getName()))));
        holder.setInventory(gui);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof com.smp.smptools.christmas.SecretSantaHolder holder) {
            UUID target = holder.getTargetUUID();
            Inventory inv = event.getInventory();

            ItemStack[] items = inv.getContents();
            manager.depositGift(target, items);
            event.getPlayer().sendMessage(plugin.getMessageManager().getMessage("secret-santa.gift-deposited"));
        }
    }
}

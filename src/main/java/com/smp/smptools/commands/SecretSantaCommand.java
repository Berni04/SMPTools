package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.SecretSantaManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SecretSantaCommand implements CommandExecutor, Listener {

    private final SMPTools plugin;
    private final SecretSantaManager manager;
    private final Map<UUID, UUID> depositSessions = new HashMap<>(); // Santa -> Target

    public SecretSantaCommand(SMPTools plugin, SecretSantaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        SecretSantaManager.Phase phase = manager.getCurrentPhase();

        switch (sub) {
            case "join":
                if (phase != SecretSantaManager.Phase.REGISTRATION) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Registration is closed!</red>"));
                    return true;
                }
                if (manager.isRegistered(player.getUniqueId())) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You are already registered!</red>"));
                    return true;
                }
                manager.registerPlayer(player.getUniqueId());
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>You have joined Secret Santa! Come back on Dec 15th to see who your target is.</green>"));
                break;

            case "target":
                if (phase == SecretSantaManager.Phase.REGISTRATION) {
                    player.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<red>Targets haven't been assigned yet! Wait until Dec 15th.</red>"));
                    return true;
                }
                UUID targetUUID = manager.getTarget(player.getUniqueId());
                if (targetUUID == null) {
                    player.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<red>You don't have a target! Did you register?</red>"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<green>You are the Secret Santa for: <gold>" + target.getName() + "</gold>!</green>"));
                break;

            case "deposit":
                if (phase != SecretSantaManager.Phase.PREPARATION) {
                    player.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<red>You can only deposit gifts between Dec 15th and Dec 24th!</red>"));
                    return true;
                }
                UUID depositTarget = manager.getTarget(player.getUniqueId());
                if (depositTarget == null) {
                    player.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>You are not part of Secret Santa.</red>"));
                    return true;
                }
                if (manager.hasGiftDeposited(depositTarget)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>You have already deposited a gift! Contact an admin if you need to change it.</red>"));
                    return true;
                }
                openDepositGUI(player, depositTarget);
                break;

            case "claim":
                if (phase != SecretSantaManager.Phase.CELEBRATION) {
                    player.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<red>You can't claim your gift yet! Wait until Christmas Day.</red>"));
                    return true;
                }
                ItemStack[] gift = manager.getGift(player.getUniqueId());
                if (gift == null || gift.length == 0) {
                    player.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<red>Your Secret Santa didn't leave you a gift... :(</red>"));
                    return true;
                }
                // Simple claim logic: dump in inventory or drop at feet
                for (ItemStack item : gift) {
                    if (item != null && item.getType() != Material.AIR) {
                        HashMap<Integer, ItemStack> left = player.getInventory().addItem(item);
                        for (ItemStack drop : left.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                    }
                }
                player.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<green>Merry Christmas! You claimed your gift!</green>"));
                // Prevent double claiming logic could be added here, but for now we assume good
                // faith or add a 'claimed' flag in manager if needed.
                break;

            case "admin":
                if (!player.hasPermission("smptools.admin")) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No permission.</red>"));
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("start")) {
                    manager.generateMatches();
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Matches generated!</green>"));
                }
                break;

            default:
                sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>--- Secret Santa ---</gold>"));
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("<yellow>/secretsanta join</yellow> - Join the event (Dec 1-15)"));
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("<yellow>/secretsanta target</yellow> - See who you are gifting to"));
        player.sendMessage(
                MiniMessage.miniMessage().deserialize("<yellow>/secretsanta deposit</yellow> - Deposit your gift"));
        player.sendMessage(MiniMessage.miniMessage()
                .deserialize("<yellow>/secretsanta claim</yellow> - Claim your gift (Dec 25)"));
    }

    private void openDepositGUI(Player player, UUID targetUUID) {
        Inventory gui = Bukkit.createInventory(null, 27, MiniMessage.miniMessage()
                .deserialize("Deposit Gift for " + Bukkit.getOfflinePlayer(targetUUID).getName()));
        depositSessions.put(player.getUniqueId(), targetUUID);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (depositSessions.containsKey(event.getPlayer().getUniqueId())) {
            UUID target = depositSessions.remove(event.getPlayer().getUniqueId());
            Inventory inv = event.getInventory();

            // Check if title matches (simple check)
            if (!event.getView().title().toString().contains("Deposit Gift"))
                return;

            ItemStack[] items = inv.getContents();
            manager.depositGift(target, items);
            event.getPlayer().sendMessage(MiniMessage.miniMessage()
                    .deserialize("<green>Gift deposited safely! Your Secret Santa duty is done.</green>"));
        }
    }
}

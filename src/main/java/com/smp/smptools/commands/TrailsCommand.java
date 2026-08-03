package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.trails.TrailType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TrailsCommand implements CommandExecutor {

    private final SMPTools plugin;

    public TrailsCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("features.trails.enabled", true)) {
            player.sendMessage("Trails feature is currently disabled.");
            return true;
        }

        openTrailsGUI(player);
        return true;
    }

    public void openTrailsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MiniMessage.miniMessage().deserialize("<gradient:blue:purple>Cosmetic Particle Trails</gradient>"));
        TrailType active = plugin.getTrailManager().getActiveTrail(player);

        int slot = 10;
        for (TrailType trail : TrailType.values()) {
            boolean hasPerm = player.hasPermission(trail.getPermission()) || player.hasPermission("smptools.trails.all");
            boolean isActive = active == trail;

            ItemStack item = new ItemStack(trail.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(MiniMessage.miniMessage().deserialize(trail.getDisplayName()));
                List<Component> lore = new ArrayList<>();
                if (isActive) {
                    lore.add(MiniMessage.miniMessage().deserialize("<green><b>EQUIPPED</b></green>"));
                    lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to remove trail</gray>"));
                } else if (hasPerm) {
                    lore.add(MiniMessage.miniMessage().deserialize("<yellow>Unlocked</yellow>"));
                    lore.add(MiniMessage.miniMessage().deserialize("<gray>Click to equip trail</gray>"));
                } else {
                    lore.add(MiniMessage.miniMessage().deserialize("<red>LOCKED</red>"));
                    lore.add(MiniMessage.miniMessage().deserialize("<dark_gray>Req perm: " + trail.getPermission() + "</dark_gray>"));
                }
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        // Slot 22: Clear trail button
        ItemStack clearItem = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clearItem.getItemMeta();
        if (clearMeta != null) {
            clearMeta.displayName(MiniMessage.miniMessage().deserialize("<red>Clear Active Trail</red>"));
            clearItem.setItemMeta(clearMeta);
        }
        inv.setItem(22, clearItem);

        player.openInventory(inv);
    }
}

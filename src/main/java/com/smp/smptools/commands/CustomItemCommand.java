package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomItemCommand extends AbstractPlayerCommand {

    public CustomItemCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.customitem")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/customitem <item_type> <custom_model_data>")));
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("custom-item.invalid-type", player,
                    java.util.Map.of("type", args[0])));
            return true;
        }

        if (!material.isItem() || material.isAir()) {
            player.sendMessage(plugin.getMessageManager().getMessage("custom-item.invalid-type", player,
                    java.util.Map.of("type", args[0])));
            return true;
        }

        int customModelData;
        try {
            customModelData = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getMessage("custom-item.invalid-data", player,
                    java.util.Map.of("data", args[1])));
            return true;
        }

        ItemStack customItem = new ItemStack(material);
        ItemMeta meta = customItem.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            customItem.setItemMeta(meta);
        }

        player.getInventory().addItem(customItem);
        player.sendMessage(plugin.getMessageManager().getMessage("custom-item.gave", player,
                    java.util.Map.of("material", material.name(), "data", String.valueOf(customModelData))));

        return true;
    }
}

package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class RenameCommand extends AbstractPlayerCommand {

    public RenameCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir()) {
            player.sendMessage(plugin.getMessageManager().getMessage("rename.holding-required"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("rename.usage"));
            player.sendMessage(plugin.getMessageManager().getMessage("rename.example"));
            player.sendMessage(plugin.getMessageManager().getMessage("rename.format-help"));
            return true;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (String arg : args) {
            nameBuilder.append(arg).append(" ");
        }
        String rawName = nameBuilder.toString().trim();

        Component newName = MiniMessage.miniMessage().deserialize(rawName);

        ItemMeta meta = itemInHand.getItemMeta();
        meta.displayName(newName);
        itemInHand.setItemMeta(meta);

        player.sendMessage(plugin.getMessageManager().getMessage("rename.success"));
        return true;
    }
}

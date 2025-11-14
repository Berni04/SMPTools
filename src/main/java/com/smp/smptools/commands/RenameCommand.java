package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RenameCommand implements CommandExecutor {

    private final SMPTools plugin;

    public RenameCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command!</red>"));
            return true;
        }

        Player player = (Player) sender;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must be holding an item to rename it!</red>"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /rename <name></red>"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Example: /rename <gradient:#FF0000:#0000FF>My Awesome Item</gradient></gray>"));
            player.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Supports MiniMessage formatting (colors, bold, italic, hex codes, etc.).</gray>"));
            return true;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (String arg : args) {
            nameBuilder.append(arg).append(" ");
        }
        String rawName = nameBuilder.toString().trim();

        // Apply MiniMessage formatting
        Component newName = MiniMessage.miniMessage().deserialize(rawName);

        ItemMeta meta = itemInHand.getItemMeta();
        meta.displayName(newName);
        itemInHand.setItemMeta(meta);

        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Item renamed successfully!</green>"));
        return true;
    }
}

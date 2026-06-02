package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class PrefixCommand extends AbstractPlayerCommand {

    public static final List<String> prefixes = Arrays.asList("❤", "♦", "⭐", "☠", "✔", "✖", "⚡", "☢");

    public PrefixCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        Inventory prefixGUI = Bukkit.createInventory(null, 9, plugin.getMessageManager().getMessage("prefix.gui-title", player));

        for (int i = 0; i < prefixes.size(); i++) {
            ItemStack item = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(prefixes.get(i)).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
            prefixGUI.setItem(i, item);
        }

        player.openInventory(prefixGUI);
        return true;
    }
}

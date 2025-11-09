package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.enchants.CustomEnchantment;
import com.smp.smptools.enchants.TelekinesisEnchant;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CustomEnchantCommand implements CommandExecutor {

    private final SMPTools plugin;
    private final Map<String, CustomEnchantment> enchantments = new HashMap<>();

    public CustomEnchantCommand(SMPTools plugin) {
        this.plugin = plugin;
        enchantments.put("telekinesis", new TelekinesisEnchant());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smptools.customenchant")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /cenchant <enchantment>");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command to enchant items.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        String enchantName = args[0].toLowerCase();

        CustomEnchantment enchant = enchantments.get(enchantName);

        if (enchant == null) {
            sender.sendMessage("§cUnknown enchantment: " + enchantName);
            return true;
        }

        if (!plugin.getEnchantmentManager().isApplicable(enchant, item.getType())) {
            sender.sendMessage("§cThat enchantment cannot be applied to this item.");
            return true;
        }

        plugin.getEnchantmentManager().applyEnchantment(item, enchant);
        sender.sendMessage("§aSuccessfully applied " + enchant.getDisplayName() + " to your item.");

        return true;
    }
}

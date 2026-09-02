package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.enchants.CustomEnchantment;
import com.smp.smptools.enchants.LumberjackEnchant;
import com.smp.smptools.enchants.TelekinesisEnchant;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CustomEnchantCommand extends AbstractPlayerCommand {

    private final Map<String, CustomEnchantment> enchantments = new HashMap<>();

    public CustomEnchantCommand(SMPTools plugin) {
        super(plugin);
        enchantments.put("telekinesis", new TelekinesisEnchant());
        enchantments.put("lumberjack", new LumberjackEnchant());
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.customenchant")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/cenchant <enchantment>")));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You must hold an item in your main hand to enchant it.</red>"));
            return true;
        }

        String enchantName = args[0].toLowerCase();
        CustomEnchantment enchant = enchantments.get(enchantName);

        if (enchant == null) {
            player.sendMessage(plugin.getMessageManager().getMessage("enchant.unknown", player, Map.of("enchant", enchantName)));
            return true;
        }

        if (!plugin.getEnchantmentManager().isApplicable(enchant, item.getType())) {
            player.sendMessage(plugin.getMessageManager().getMessage("enchant.not-applicable"));
            return true;
        }

        if (plugin.getEnchantmentManager().hasEnchantment(item, enchant)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>This item already has " + enchant.getDisplayName() + "!</red>"));
            return true;
        }

        plugin.getEnchantmentManager().applyEnchantment(item, enchant);
        player.sendMessage(plugin.getMessageManager().getMessage("enchant.applied", player, Map.of("enchant", enchant.getDisplayName())));

        return true;
    }
}

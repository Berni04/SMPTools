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

    private static final MiniMessage SAFE_MINI_MESSAGE = MiniMessage.builder()
            .tags(net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.builder()
                    .resolver(net.kyori.adventure.text.minimessage.tag.standard.StandardTags.color())
                    .resolver(net.kyori.adventure.text.minimessage.tag.standard.StandardTags.decorations())
                    .resolver(net.kyori.adventure.text.minimessage.tag.standard.StandardTags.gradient())
                    .resolver(net.kyori.adventure.text.minimessage.tag.standard.StandardTags.rainbow())
                    .build())
            .build();

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.rename")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

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

        if (rawName.length() > 64) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Item name cannot exceed 64 characters.</red>"));
            return true;
        }

        Component newName = SAFE_MINI_MESSAGE.deserialize(rawName);

        ItemMeta meta = itemInHand.getItemMeta();
        if (meta != null) {
            meta.displayName(newName);
            itemInHand.setItemMeta(meta);
        }

        player.sendMessage(plugin.getMessageManager().getMessage("rename.success"));
        return true;
    }
}

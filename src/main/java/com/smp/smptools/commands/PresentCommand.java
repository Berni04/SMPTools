package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.christmas.PresentManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PresentCommand extends AbstractPlayerCommand implements TabCompleter {

    private final PresentManager presentManager;

    public PresentCommand(SMPTools plugin, PresentManager presentManager) {
        super(plugin);
        this.presentManager = presentManager;
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/present <give|remove> [tier]")));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("give")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                        java.util.Map.of("usage", "/present give <tier>")));
                return true;
            }
            String tier = args[1].toLowerCase();
            ItemStack presentItem = presentManager.getPresentItem(tier);

            if (presentItem == null) {
                player.sendMessage(plugin.getMessageManager().getMessage("present.invalid-tier", player,
                        java.util.Map.of("tier", tier)));
                return true;
            }

            player.getInventory().addItem(presentItem);
            player.sendMessage(plugin.getMessageManager().getMessage("present.given", player,
                    java.util.Map.of("tier", tier)));

        } else if (subCommand.equals("remove")) {
            if (presentManager.getPresentIdAt(player.getTargetBlock(null, 5).getLocation()) != null) {
                presentManager.removePresent(player.getTargetBlock(null, 5).getLocation());
                player.getTargetBlock(null, 5).setType(org.bukkit.Material.AIR);
                player.sendMessage(plugin.getMessageManager().getMessage("present.removed"));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("present.no-present"));
            }
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("common.unknown-subcommand", player,
                    java.util.Map.of("usage", "/present <give|remove> [tier]")));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("give");
            completions.add("remove");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(presentManager.getTiers());
        }
        return completions;
    }
}

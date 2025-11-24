package com.smp.smptools.commands;

import com.smp.smptools.christmas.PresentManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PresentCommand implements CommandExecutor, TabCompleter {

    private final PresentManager presentManager;

    public PresentCommand(PresentManager presentManager) {
        this.presentManager = presentManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("smptools.admin")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /present <give|remove> [tier]", NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("give")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("Usage: /present give <tier>", NamedTextColor.RED));
                return true;
            }
            String tier = args[1].toLowerCase();
            ItemStack presentItem = presentManager.getPresentItem(tier);

            if (presentItem == null) {
                player.sendMessage(Component.text("Invalid present tier: " + tier, NamedTextColor.RED));
                return true;
            }

            player.getInventory().addItem(presentItem);
            player.sendMessage(Component.text("Given 1 " + tier + " present.", NamedTextColor.GREEN));

        } else if (subCommand.equals("remove")) {
            if (presentManager.getPresentIdAt(player.getTargetBlock(null, 5).getLocation()) != null) {
                presentManager.removePresent(player.getTargetBlock(null, 5).getLocation());
                player.getTargetBlock(null, 5).setType(org.bukkit.Material.AIR);
                player.sendMessage(Component.text("Present removed.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("No present found at target block.", NamedTextColor.RED));
            }
        } else {
            player.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
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

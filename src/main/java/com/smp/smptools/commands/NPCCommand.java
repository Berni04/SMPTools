package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NPCCommand implements CommandExecutor {

    private final SMPTools plugin;

    public NPCCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("smptools.npc.admin")) {
            player.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /npc <spawn|remove|reload>", NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn":
                if (args.length < 2) {
                    player.sendMessage(
                            Component.text("Usage: /npc spawn <id> [type] [skin] [name]", NamedTextColor.RED));
                    return true;
                }
                String id = args[1];
                String type = args.length > 2 ? args[2].toUpperCase() : "DIALOGUE";
                String skin = args.length > 3 ? args[3] : "Steve";
                String name = null;
                if (args.length > 4) {
                    // Join remaining args for name
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 4; i < args.length; i++) {
                        nameBuilder.append(args[i]).append(" ");
                    }
                    name = nameBuilder.toString().trim();
                }

                plugin.getNPCManager().createNPC(id, player.getLocation(), type, skin, name);
                player.sendMessage(Component.text("NPC " + id + " spawned!", NamedTextColor.GREEN));
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /npc remove <id>", NamedTextColor.RED));
                    return true;
                }
                String removeId = args[1];
                plugin.getNPCManager().removeNPC(removeId);
                player.sendMessage(Component.text("NPC " + removeId + " removed!", NamedTextColor.GREEN));
                break;

            case "reload":
                plugin.getNPCManager().loadNPCs();
                player.sendMessage(Component.text("NPCs reloaded!", NamedTextColor.GREEN));
                break;

            case "respond":
                if (args.length < 3) {
                    player.sendMessage(Component.text("Usage: /npc respond <dialogueId> <lineId> <optionIndex>",
                            NamedTextColor.RED));
                    return true;
                }
                String dialogueId = args[1];
                String lineId = args[2];
                int optionIndex;
                try {
                    optionIndex = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid option index.", NamedTextColor.RED));
                    return true;
                }
                plugin.getDialogueManager().handleOptionSelection(player, dialogueId, lineId, optionIndex);
                break;

            default:
                player.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
                break;
        }

        return true;
    }
}

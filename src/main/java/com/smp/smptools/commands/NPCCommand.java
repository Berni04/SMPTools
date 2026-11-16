package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class NPCCommand extends AbstractPlayerCommand {

    public NPCCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (!player.hasPermission("smptools.npc.admin")) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/npc <spawn|remove|reload>")));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "spawn":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                            java.util.Map.of("usage", "/npc spawn <id> [type] [skin] [name]")));
                    return true;
                }
                String id = args[1];
                if (!id.matches("^[a-zA-Z0-9_-]+$")) {
                    player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Invalid NPC ID. Use letters, numbers, hyphens, and underscores only.</red>"));
                    return true;
                }
                String type = args.length > 2 ? args[2].toUpperCase() : "DIALOGUE";
                String skin = args.length > 3 ? args[3] : "Steve";
                if (!skin.matches("^[a-zA-Z0-9_-]+$")) {
                    skin = "Steve";
                }
                String name = null;
                if (args.length > 4) {
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 4; i < args.length; i++) {
                        nameBuilder.append(args[i]).append(" ");
                    }
                    name = nameBuilder.toString().trim();
                }

                plugin.getNPCManager().createNPC(id, player.getLocation(), type, skin, name);
                player.sendMessage(plugin.getMessageManager().getMessage("npc.spawned", player,
                        java.util.Map.of("id", id)));
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                            java.util.Map.of("usage", "/npc remove <id>")));
                    return true;
                }
                String removeId = args[1];
                plugin.getNPCManager().removeNPC(removeId);
                player.sendMessage(plugin.getMessageManager().getMessage("npc.removed", player,
                        java.util.Map.of("id", removeId)));
                break;

            case "reload":
                plugin.getNPCManager().loadNPCs();
                player.sendMessage(plugin.getMessageManager().getMessage("npc.reloaded"));
                break;

            case "respond":
                if (args.length < 3) {
                    player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                            java.util.Map.of("usage", "/npc respond <dialogueId> <lineId> <optionIndex>")));
                    return true;
                }
                String dialogueId = args[1];
                String lineId = args[2];
                int optionIndex;
                try {
                    optionIndex = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getMessageManager().getMessage("npc.invalid-option-index"));
                    return true;
                }
                plugin.getDialogueManager().handleOptionSelection(player, dialogueId, lineId, optionIndex);
                break;

            default:
                player.sendMessage(plugin.getMessageManager().getMessage("common.unknown-subcommand", player,
                        java.util.Map.of("usage", "/npc <spawn|remove|reload>")));
                break;
        }

        return true;
    }
}

package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.listeners.BountyGUIListener;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BountyCommand implements CommandExecutor {

    private final SMPTools plugin;
    private final BountyGUIListener guiListener;

    public BountyCommand(SMPTools plugin, BountyGUIListener guiListener) {
        this.plugin = plugin;
        this.guiListener = guiListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("features.bounties.enabled", true)) {
            player.sendMessage("Bounties feature is currently disabled.");
            return true;
        }

        if (args.length == 0) {
            guiListener.openBountyListGUI(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "place":
            case "add":
                if (args.length < 2) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /bounty place <player></red>"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found.</red>"));
                    return true;
                }

                if (target.equals(player)) {
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You cannot place a bounty on yourself.</red>"));
                    return true;
                }

                guiListener.openPlaceGUI(player, target);
                break;

            case "claim":
            case "claims":
            case "refund":
                guiListener.openClaimGUI(player);
                break;

            case "list":
            default:
                guiListener.openBountyListGUI(player);
                break;
        }

        return true;
    }
}

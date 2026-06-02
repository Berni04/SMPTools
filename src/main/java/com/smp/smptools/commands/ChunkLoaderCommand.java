package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.chunkloaders.ChunkLoaderManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ChunkLoaderCommand implements CommandExecutor {

    private final SMPTools plugin;

    public ChunkLoaderCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smptools.chunkloader.give")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.usage",
                    sender instanceof Player ? (Player) sender : null,
                    Map.of("usage", "/givechunkloader <player>")));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return true;
        }

        ItemStack chunkLoaderItem = ChunkLoaderManager.getChunkLoaderItem();
        targetPlayer.getInventory().addItem(chunkLoaderItem);
        targetPlayer.sendMessage(plugin.getMessageManager().getMessage("chunk-loader.received"));
        sender.sendMessage(plugin.getMessageManager().getMessage("chunk-loader.gave",
                sender instanceof Player ? (Player) sender : null,
                Map.of("target", targetPlayer.getName())));

        return true;
    }
}

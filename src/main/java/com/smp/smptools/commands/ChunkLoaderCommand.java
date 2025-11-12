package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.chunkloaders.ChunkLoaderManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ChunkLoaderCommand implements CommandExecutor {

    private final SMPTools plugin;

    public ChunkLoaderCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("smptools.chunkloader.give")) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to use this command.</red>"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /givechunkloader <player></red>"));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found.</red>"));
            return true;
        }

        ItemStack chunkLoaderItem = ChunkLoaderManager.getChunkLoaderItem(); // Access via static method
        targetPlayer.getInventory().addItem(chunkLoaderItem);
        targetPlayer.sendMessage(MiniMessage.miniMessage().deserialize("<green>You received a Chunk Loader!</green>"));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Gave a Chunk Loader to " + targetPlayer.getName() + ".</green>"));

        return true;
    }
}

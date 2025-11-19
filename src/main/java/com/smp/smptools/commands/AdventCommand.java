package com.smp.smptools.commands;

import com.smp.smptools.listeners.AdventGUIListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdventCommand implements CommandExecutor {

    private final AdventGUIListener guiListener;

    public AdventCommand(AdventGUIListener guiListener) {
        this.guiListener = guiListener;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        guiListener.openAdventGUI(player);
        return true;
    }
}

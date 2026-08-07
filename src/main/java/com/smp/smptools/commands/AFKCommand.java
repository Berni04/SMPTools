package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AFKCommand implements CommandExecutor {

    private final SMPTools plugin;

    public AFKCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        if (!plugin.getConfig().getBoolean("features.afk.enabled", true)) {
            player.sendMessage("AFK feature is currently disabled.");
            return true;
        }

        boolean current = plugin.getAFKManager().isAFK(player);
        plugin.getAFKManager().setAFK(player, !current, true);
        return true;
    }
}

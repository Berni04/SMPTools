package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class DelHomeCommand extends AbstractPlayerCommand {

    public DelHomeCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player,
                    java.util.Map.of("usage", "/delhome <name>")));
            return true;
        }

        String playerUUID = player.getUniqueId().toString();
        String homeName = args[0].toLowerCase();

        if (plugin.getConfig().contains("homes." + playerUUID + "." + homeName)) {
            plugin.getConfig().set("homes." + playerUUID + "." + homeName, null);
            plugin.saveConfig();
            player.sendMessage(plugin.getMessageManager().getMessage("homes.deleted", player,
                    java.util.Map.of("name", homeName)));
        } else {
            player.sendMessage(plugin.getMessageManager().getMessage("homes.not-found", player,
                    java.util.Map.of("name", homeName)));
        }
        return true;
    }
}

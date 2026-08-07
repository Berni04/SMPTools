package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.listeners.AdventGUIListener;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class AdventCommand extends AbstractPlayerCommand {

    private final AdventGUIListener guiListener;

    public AdventCommand(SMPTools plugin, AdventGUIListener guiListener) {
        super(plugin);
        this.guiListener = guiListener;
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (guiListener == null) {
            player.sendMessage("Advent calendar feature is currently disabled.");
            return true;
        }
        guiListener.openAdventGUI(player);
        return true;
    }
}

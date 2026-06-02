package com.smp.smptools.sleep;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.AbstractPlayerCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class SleepVoteCommand extends AbstractPlayerCommand {

    private final SleepManager sleepManager;

    public SleepVoteCommand(SMPTools plugin) {
        super(plugin);
        this.sleepManager = plugin.getSleepManager();
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        String vote = args[0].toLowerCase();

        if (vote.equals("accept")) {
            sleepManager.addVote(player, true);
        } else if (vote.equals("deny")) {
            sleepManager.addVote(player, false);
        }

        return true;
    }
}

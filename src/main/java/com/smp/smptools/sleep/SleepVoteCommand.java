package com.smp.smptools.sleep;

import com.smp.smptools.SMPTools;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SleepVoteCommand implements CommandExecutor {

    private final SleepManager sleepManager;

    public SleepVoteCommand(SMPTools plugin) {
        this.sleepManager = plugin.getSleepManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            return false; // Should not happen with clickable chat
        }

        Player player = (Player) sender;
        String vote = args[0].toLowerCase();

        if (vote.equals("accept")) {
            sleepManager.addVote(player, true);
        } else if (vote.equals("deny")) {
            sleepManager.addVote(player, false);
        }

        return true;
    }
}

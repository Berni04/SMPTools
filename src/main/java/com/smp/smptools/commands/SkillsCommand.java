package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.skills.SkillType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SkillsCommand implements CommandExecutor {

    private final SMPTools plugin;

    public SkillsCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        player.sendMessage("§a--- Your Skills ---");
        for (SkillType skill : SkillType.values()) {
            int level = plugin.getSkillsManager().getLevel(player, skill);
            player.sendMessage("§e" + skill.getDisplayName() + ": §fLevel " + level);
        }

        return true;
    }
}

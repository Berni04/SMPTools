package com.smp.smptools.events.seasonal.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.seasonal.SeasonalManager;
import com.smp.smptools.events.seasonal.gui.HalloweenGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command handler for /halloween.
 */
public class HalloweenCommand implements CommandExecutor, TabCompleter {

    private final SMPTools plugin;
    private final SeasonalManager seasonalManager;
    private final HalloweenGUI halloweenGUI;

    public HalloweenCommand(SMPTools plugin, SeasonalManager seasonalManager, HalloweenGUI halloweenGUI) {
        this.plugin = plugin;
        this.seasonalManager = seasonalManager;
        this.halloweenGUI = halloweenGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                halloweenGUI.openGUI(player);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can open the Halloween Checklist.</red>"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("setpumpkin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only in-game players can set pumpkin locations.</red>"));
                return true;
            }
            if (!player.hasPermission("smptools.seasonal.admin")) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to set pumpkins.</red>"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /halloween setpumpkin <id> [hint text]</red>"));
                return true;
            }

            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Pumpkin ID must be a number (1-20).</red>"));
                return true;
            }

            Block target = player.getTargetBlockExact(5);
            if (target == null || target.getType().isAir()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Look at a block within 5 blocks to set it as a pumpkin target!</red>"));
                return true;
            }

            String hint = "Hidden near " + target.getWorld().getName() + " coordinates " + target.getX() + ", " + target.getZ();
            if (args.length >= 3) {
                hint = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            }

            target.setType(Material.JACK_O_LANTERN);
            seasonalManager.setPumpkinLocation(id, target.getLocation(), hint);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Set Pumpkin #" + id + " at " + target.getX() + ", " + target.getY() + ", " + target.getZ() + " with hint: '" + hint + "'</green>"));
            return true;
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand. Use /halloween or /halloween setpumpkin <id> [hint].</red>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("smptools.seasonal.admin")) {
            completions.add("setpumpkin");
        }
        return completions;
    }
}

package com.smp.smptools.artifacts.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.artifacts.ArtifactManager;
import com.smp.smptools.artifacts.ArtifactType;
import com.smp.smptools.artifacts.gui.ArtifactEquipmentGUI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Command handler for /artifacts and /artifacts give.
 */
public class ArtifactCommand implements CommandExecutor, TabCompleter {

    private final SMPTools plugin;
    private final ArtifactManager artifactManager;
    private final ArtifactEquipmentGUI artifactGUI;

    public ArtifactCommand(SMPTools plugin, ArtifactManager artifactManager, ArtifactEquipmentGUI artifactGUI) {
        this.plugin = plugin;
        this.artifactManager = artifactManager;
        this.artifactGUI = artifactGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                artifactGUI.openGUI(player);
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can open the Artifact Equipment Pouch.</red>"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("smptools.artifacts.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to give artifacts.</red>"));
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /artifacts give <player> <artifact_type></red>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found: " + args[1] + "</red>"));
                return true;
            }

            ArtifactType type;
            try {
                type = ArtifactType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid artifact type. Available: " + Arrays.toString(ArtifactType.values()) + "</red>"));
                return true;
            }

            ItemStack artifactItem = artifactManager.createArtifact(type);
            Map<Integer, ItemStack> leftover = target.getInventory().addItem(artifactItem);
            if (!leftover.isEmpty()) {
                for (ItemStack dropped : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), dropped);
                }
                target.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Your inventory was full! Artifact was dropped at your feet.</yellow>"));
            }
            target.sendMessage(MiniMessage.miniMessage().deserialize("<green>You received the artifact: </green>" + type.getFormattedName()));
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Gave artifact " + type.getFormattedName() + " to " + target.getName() + "</green>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("claim")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can claim overflow artifacts.</red>"));
                return true;
            }
            List<ItemStack> recovered = artifactManager.claimPouchOverflow(player);
            if (recovered.isEmpty()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>No overflow artifacts to claim.</yellow>"));
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Successfully claimed " + recovered.size() + " recovered artifact(s) (check your inventory or nearby ground)!</green>"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("recover")) {
            if (!sender.hasPermission("smptools.artifacts.admin")) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You don't have permission to recover artifacts.</red>"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /artifacts recover <player></red>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player not found: " + args[1] + "</red>"));
                return true;
            }
            List<ItemStack> recovered = artifactManager.claimPouchOverflow(target);
            if (recovered.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>No overflow artifacts found for " + target.getName() + ".</yellow>"));
            } else {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Recovered and delivered " + recovered.size() + " artifact(s) to " + target.getName() + " (check inventory or nearby ground).</green>"));
                target.sendMessage(MiniMessage.miniMessage().deserialize("<green>An administrator recovered " + recovered.size() + " artifact(s) for you; check your inventory or nearby ground.</green>"));
            }
            return true;
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown subcommand. Use /artifacts, /artifacts claim, /artifacts give <player> <type>, or /artifacts recover <player>.</red>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("claim");
            if (sender.hasPermission("smptools.artifacts.admin")) {
                completions.add("give");
                completions.add("recover");
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("recover"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (ArtifactType t : ArtifactType.values()) {
                completions.add(t.name().toLowerCase());
            }
        }
        return completions;
    }
}

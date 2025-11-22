package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.listeners.MissionGUIListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import com.smp.smptools.utils.HeadUtils;
import org.bukkit.Material;
import org.bukkit.Color;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class MissionCommand implements CommandExecutor {

    private final SMPTools plugin;
    public static final NamespacedKey MISSION_NPC_KEY = new NamespacedKey(SMPTools.getInstance(), "mission_npc");
    public static final NamespacedKey SANTA_NPC_KEY = new NamespacedKey(SMPTools.getInstance(), "santa_npc");
    public static final Component NPC_NAME = Component.text("Quest Master", NamedTextColor.GOLD, TextDecoration.BOLD);
    public static final Component SANTA_NAME = Component.text("Santa Claus", NamedTextColor.RED, TextDecoration.BOLD);

    public MissionCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            // Admin commands
            if (args[0].equalsIgnoreCase("npc")) {
                player.sendMessage(Component.text("Use /npc command for NPC management.", NamedTextColor.YELLOW));
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                handleListCommand(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("complete")) {
                handleCompleteCommand(player, args);
                return true;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                handleResetCommand(player, args);
                return true;
            }

            if (args[0].equalsIgnoreCase("santa")) {
                handleSantaCommand(player);
                return true;
            }

            if (args[0].equalsIgnoreCase("resetquestline")) {
                handleResetQuestlineCommand(player, args);
                return true;
            }
        }

        // Default action: open the mission GUI
        MissionGUIListener.openMissionGUI(player, false, "NORMAL");
        return true;
    }

    // NPC spawning logic moved to NPCCommand and NPCManager

    private void handleListCommand(Player admin) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }

        admin.sendMessage(Component.text("Available Mission IDs:", NamedTextColor.GOLD));
        for (String missionId : plugin.getMissionManager().getAllMissions().keySet()) {
            admin.sendMessage(Component.text("- " + missionId, NamedTextColor.YELLOW)
                    .hoverEvent(Component.text("Click to copy"))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(missionId)));
        }
    }

    private void handleCompleteCommand(Player admin, String[] args) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            admin.sendMessage(Component.text("Usage: /missions complete <player> <missionId>", NamedTextColor.RED));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        String missionId = args[2];
        if (plugin.getMissionManager().getMission(missionId) == null) {
            admin.sendMessage(Component.text("Mission ID not found.", NamedTextColor.RED));
            return;
        }

        plugin.getMissionManager().forceCompleteMission(target, missionId);
        admin.sendMessage(Component.text("Forcibly completed mission '" + missionId + "' for " + target.getName(),
                NamedTextColor.GREEN));
    }

    private void handleResetCommand(Player admin, String[] args) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            admin.sendMessage(Component.text("Usage: /missions reset <player> <missionId>", NamedTextColor.RED));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        String missionId = args[2];
        if (plugin.getMissionManager().getMission(missionId) == null) {
            admin.sendMessage(Component.text("Mission ID not found.", NamedTextColor.RED));
            return;
        }

        plugin.getMissionManager().resetMission(target, missionId);
        admin.sendMessage(
                Component.text("Reset mission '" + missionId + "' for " + target.getName(), NamedTextColor.GREEN));
    }

    private void handleSantaCommand(Player player) {
        com.smp.smptools.missions.MissionManager.PlayerMissionData playerData = plugin.getMissionManager()
                .getPlayerData(player);
        String selectedQuestline = playerData.getSelectedQuestline();

        if (selectedQuestline != null) {
            MissionGUIListener.openMissionGUI(player, true, selectedQuestline);
        } else {
            MissionGUIListener.openQuestlineSelectionGUI(player);
        }
    }

    private void handleResetQuestlineCommand(Player admin, String[] args) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            admin.sendMessage(Component.text("Usage: /missions resetquestline <player>", NamedTextColor.RED));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }

        com.smp.smptools.missions.MissionManager.PlayerMissionData data = plugin.getMissionManager()
                .getPlayerData(target);
        String currentQuestline = data.getSelectedQuestline();

        if (currentQuestline != null) {
            // Reset all missions in this questline
            for (com.smp.smptools.missions.Mission mission : plugin.getMissionManager().getAllMissions().values()) {
                if (mission.getCategory().equalsIgnoreCase(currentQuestline)) {
                    plugin.getMissionManager().resetMission(target, mission.getId());
                }
            }
        }

        data.setSelectedQuestline(null);
        plugin.getMissionManager().savePlayerData();
        admin.sendMessage(
                Component.text("Reset questline selection and progress for " + target.getName(), NamedTextColor.GREEN));
    }
}

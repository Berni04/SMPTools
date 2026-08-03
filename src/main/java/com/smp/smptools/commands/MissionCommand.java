package com.smp.smptools.commands;

import com.smp.smptools.SMPTools;
import com.smp.smptools.listeners.MissionGUIListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.util.Map;

public class MissionCommand extends AbstractPlayerCommand {

    public static final NamespacedKey MISSION_NPC_KEY = new NamespacedKey(SMPTools.getInstance(), "mission_npc");
    public static final NamespacedKey SANTA_NPC_KEY = new NamespacedKey(SMPTools.getInstance(), "santa_npc");
    public static final Component NPC_NAME = Component.text("Quest Master", NamedTextColor.GOLD, TextDecoration.BOLD);
    public static final Component SANTA_NAME = Component.text("Santa Claus", NamedTextColor.RED, TextDecoration.BOLD);

    public MissionCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("npc")) {
                player.sendMessage(plugin.getMessageManager().getMessage("missions.use-npc-command", player));
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

        MissionGUIListener.openMissionGUI(player, false, "NORMAL");
        return true;
    }

    private void handleListCommand(Player admin) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return;
        }

        admin.sendMessage(plugin.getMessageManager().getMessage("missions.available-ids", admin));
        Component clickToCopy = plugin.getMessageManager().getMessage("missions.click-to-copy", admin);
        for (String missionId : plugin.getMissionManager().getAllMissions().keySet()) {
            admin.sendMessage(plugin.getMessageManager().getMessage("missions.list-item", admin, Map.of("id", missionId))
                    .hoverEvent(clickToCopy)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(missionId)));
        }
    }

    private void handleCompleteCommand(Player admin, String[] args) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return;
        }
        if (args.length < 3) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.usage", admin,
                    java.util.Map.of("usage", "/missions complete <player> <missionId>")));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return;
        }
        String missionId = args[2];
        if (plugin.getMissionManager().getMission(missionId) == null) {
            admin.sendMessage(plugin.getMessageManager().getMessage("missions.id-not-found", admin));
            return;
        }

        plugin.getMissionManager().forceCompleteMission(target, missionId);
        admin.sendMessage(plugin.getMessageManager().getMessage("missions.force-completed", admin,
                java.util.Map.of("mission", missionId, "target", target.getName())));
    }

    private void handleResetCommand(Player admin, String[] args) {
        if (!admin.hasPermission("smptools.missions.admin")) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return;
        }
        if (args.length < 3) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.usage", admin,
                    java.util.Map.of("usage", "/missions reset <player> <missionId>")));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return;
        }
        String missionId = args[2];
        if (plugin.getMissionManager().getMission(missionId) == null) {
            admin.sendMessage(plugin.getMessageManager().getMessage("missions.id-not-found", admin));
            return;
        }

        plugin.getMissionManager().resetMission(target, missionId);
        admin.sendMessage(plugin.getMessageManager().getMessage("missions.reset", admin,
                java.util.Map.of("mission", missionId, "target", target.getName())));
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
            admin.sendMessage(plugin.getMessageManager().getMessage("common.no-permission"));
            return;
        }
        if (args.length < 2) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.usage", admin,
                    java.util.Map.of("usage", "/missions resetquestline <player>")));
            return;
        }
        Player target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            admin.sendMessage(plugin.getMessageManager().getMessage("common.player-not-found"));
            return;
        }

        com.smp.smptools.missions.MissionManager.PlayerMissionData data = plugin.getMissionManager()
                .getPlayerData(target);
        String currentQuestline = data.getSelectedQuestline();

        if (currentQuestline != null) {
            for (com.smp.smptools.missions.Mission mission : plugin.getMissionManager().getAllMissions().values()) {
                if (mission.getCategory().equalsIgnoreCase(currentQuestline)) {
                    plugin.getMissionManager().resetMission(target, mission.getId());
                }
            }
        }

        data.setSelectedQuestline(null);
        plugin.getMissionManager().savePlayerData();
        admin.sendMessage(plugin.getMessageManager().getMessage("missions.questline-reset", admin,
                java.util.Map.of("target", target.getName())));
    }
}

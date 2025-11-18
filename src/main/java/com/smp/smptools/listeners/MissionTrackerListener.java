package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.missions.Mission;
import com.smp.smptools.missions.MissionManager;
import com.smp.smptools.missions.MissionType;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;

public class MissionTrackerListener implements Listener {

    private final MissionManager missionManager;

    public MissionTrackerListener(SMPTools plugin) {
        this.missionManager = plugin.getMissionManager();
    }

    @EventHandler
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        Player player = event.getPlayer();
        Statistic statistic = event.getStatistic();

        if (statistic != Statistic.AVIATE_ONE_CM) {
            return;
        }

        MissionManager.PlayerMissionData playerData = missionManager.getPlayerData(player);

        for (String missionId : playerData.getActiveMissions()) {
            Mission mission = missionManager.getMission(missionId);
            if (mission != null && mission.getType() == MissionType.STATISTIC && mission.getObjective().equals("AVIATE_ONE_CM")) {
                int newAmount = event.getNewValue();
                playerData.getMissionProgress().put(missionId, newAmount);
                // The GUI will be responsible for checking if this amount meets the goal
            }
        }
    }
}

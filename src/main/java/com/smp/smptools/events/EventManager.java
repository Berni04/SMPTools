package com.smp.smptools.events;

import com.smp.smptools.SMPTools;
import com.smp.smptools.events.minievents.MiniEventListener;
import com.smp.smptools.events.minievents.MiniEventSession;
import com.smp.smptools.events.minievents.MiniEventType;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

/**
 * Core event management engine for SMPTools.
 * Manages automated event timers, active event state, and event listeners.
 */
public class EventManager {

    private final SMPTools plugin;
    private MiniEventSession activeSession;
    private BukkitTask autoScheduleTask;
    private final Random random = new Random();

    public EventManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // Register listener
        Bukkit.getPluginManager().registerEvents(new MiniEventListener(plugin, this), plugin);

        // Start automated scheduler if enabled
        if (plugin.getEventsConfig().getBoolean("events.enabled", true)) {
            int intervalMinutes = plugin.getEventsConfig().getInt("events.interval-minutes", 120);
            long periodTicks = intervalMinutes * 60 * 20L;

            autoScheduleTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (activeSession == null || !activeSession.isActive()) {
                        java.util.List<MiniEventType> enabledTypes = new java.util.ArrayList<>();
                        for (MiniEventType t : MiniEventType.values()) {
                            if (plugin.getEventsConfig().getBoolean("events.types." + t.getConfigKey() + ".enabled", true)) {
                                enabledTypes.add(t);
                            }
                        }
                        if (!enabledTypes.isEmpty()) {
                            MiniEventType randomType = enabledTypes.get(random.nextInt(enabledTypes.size()));
                            int duration = plugin.getEventsConfig().getInt("events.types." + randomType.getConfigKey() + ".duration-minutes", 15);
                            startEvent(randomType, duration);
                        }
                    }
                }
            }.runTaskTimer(plugin, periodTicks, periodTicks);
        }
    }

    public boolean startEvent(MiniEventType type, int durationMinutes) {
        if (activeSession != null && activeSession.isActive()) {
            return false;
        }

        activeSession = new MiniEventSession(plugin, type, durationMinutes);
        activeSession.start();
        return true;
    }

    public boolean stopActiveEvent() {
        if (activeSession == null || !activeSession.isActive()) {
            return false;
        }

        activeSession.end();
        activeSession = null;
        return true;
    }

    public MiniEventSession getActiveSession() {
        return activeSession;
    }

    public void shutdown() {
        if (autoScheduleTask != null) {
            autoScheduleTask.cancel();
        }
        if (activeSession != null && activeSession.isActive()) {
            activeSession.end();
        }
    }
}

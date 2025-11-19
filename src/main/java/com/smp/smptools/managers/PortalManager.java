package com.smp.smptools.managers;

import com.smp.smptools.SMPTools;
import org.bukkit.Location;
import org.bukkit.World;

public class PortalManager {

    private final SMPTools plugin;

    public PortalManager(SMPTools plugin) {
        this.plugin = plugin;
    }

    public Location getChristmasSpawn() {
        World world = plugin.getChristmasWorldManager().getChristmasWorld();
        if (world != null) {
            return world.getSpawnLocation();
        }
        return null;
    }
}

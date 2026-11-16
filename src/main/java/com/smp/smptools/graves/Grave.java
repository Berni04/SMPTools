package com.smp.smptools.graves;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class Grave {
    private final UUID owner;
    private final String ownerName;
    private final Location location;
    private final String worldName;
    private final List<ItemStack> items;
    private final long timeOfDeath;
    private final String causeOfDeath;

    public Grave(UUID owner, String ownerName, Location location, List<ItemStack> items, long timeOfDeath,
            String causeOfDeath) {
        this(owner, ownerName, location, extractWorldName(location), items, timeOfDeath, causeOfDeath);
    }

    public Grave(UUID owner, String ownerName, Location location, String worldName, List<ItemStack> items, long timeOfDeath,
            String causeOfDeath) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.location = location;
        this.worldName = worldName != null ? worldName : extractWorldName(location);
        this.items = items;
        this.timeOfDeath = timeOfDeath;
        this.causeOfDeath = causeOfDeath;
    }

    private static String extractWorldName(Location loc) {
        if (loc == null) return null;
        try {
            if (loc.isWorldLoaded() && loc.getWorld() != null) {
                return loc.getWorld().getName();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public String getWorldName() {
        return worldName;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Location getLocation() {
        return location;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public long getTimeOfDeath() {
        return timeOfDeath;
    }

    public String getCauseOfDeath() {
        return causeOfDeath;
    }
}

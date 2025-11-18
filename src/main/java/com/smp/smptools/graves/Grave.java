package com.smp.smptools.graves;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class Grave {
    private final UUID owner;
    private final String ownerName;
    private final Location location;
    private final List<ItemStack> items;
    private final long timeOfDeath;
    private final String causeOfDeath;

    public Grave(UUID owner, String ownerName, Location location, List<ItemStack> items, long timeOfDeath,
            String causeOfDeath) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.location = location;
        this.items = items;
        this.timeOfDeath = timeOfDeath;
        this.causeOfDeath = causeOfDeath;
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

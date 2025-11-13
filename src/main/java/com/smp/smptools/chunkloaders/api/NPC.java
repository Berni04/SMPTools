package com.smp.smptools.chunkloaders.api;

import org.bukkit.Location;
import org.bukkit.profile.PlayerProfile;

import java.util.UUID;

public interface NPC {

    /**
     * Spawns the NPC at its current location.
     */
    void spawn();

    /**
     * Despawns the NPC.
     */
    void despawn();

    /**
     * Teleports the NPC to a new location.
     * @param location The new location.
     */
    void teleport(Location location);

    /**
     * Makes the NPC look at a specific location.
     * @param location The location to look at.
     */
    void lookAt(Location location);

    /**
     * Gets the current location of the NPC.
     * @return The NPC's location.
     */
    Location getLocation();

    /**
     * Gets the display name of the NPC.
     * @return The NPC's name.
     */
    String getName();

    /**
     * Gets the unique ID of the NPC.
     * @return The NPC's UUID.
     */
    UUID getUniqueId();

    /**
     * Gets the PlayerProfile of the NPC, which includes its skin.
     * @return The NPC's PlayerProfile.
     */
    PlayerProfile getProfile();

    /**
     * Checks if the NPC is currently spawned and visible to players.
     * @return True if spawned, false otherwise.
     */
    boolean isSpawned();

    // You can add more methods here for interactions, equipment, etc.
}
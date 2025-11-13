package com.smp.smptools.chunkloaders.api;

import com.smp.smptools.chunkloaders.internal.NPCImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.profile.PlayerProfile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakePlayerAPI {

    private static final Map<UUID, NPC> npcs = new HashMap<>();

    /**
     * Creates a new NPC.
     * @param location The initial location of the NPC.
     * @param name The display name of the NPC.
     * @param profile The PlayerProfile for the NPC (including skin).
     * @return The created NPC object.
     */
    public static NPC createNPC(Location location, String name, PlayerProfile profile) {
        UUID uuid = UUID.randomUUID(); // Generate a random UUID for the NPC
        NPCImpl npc = new NPCImpl(uuid, name, profile, location);
        npcs.put(uuid, npc);
        return npc;
    }

    /**
     * Creates a new NPC with a specific UUID.
     * @param uuid The UUID for the NPC.
     * @param location The initial location of the NPC.
     * @param name The display name of the NPC.
     * @param profile The PlayerProfile for the NPC (including skin).
     * @return The created NPC object.
     */
    public static NPC createNPC(UUID uuid, Location location, String name, PlayerProfile profile) {
        if (npcs.containsKey(uuid)) {
            throw new IllegalArgumentException("NPC with UUID " + uuid + " already exists.");
        }
        NPCImpl npc = new NPCImpl(uuid, name, profile, location);
        npcs.put(uuid, npc);
        return npc;
    }

    /**
     * Gets an NPC by its UUID.
     * @param uuid The UUID of the NPC.
     * @return The NPC, or null if not found.
     */
    public static NPC getNPC(UUID uuid) {
        return npcs.get(uuid);
    }

    /**
     * Gets all currently managed NPCs.
     * @return A collection of all NPCs.
     */
    public static Collection<NPC> getAllNPCs() {
        return npcs.values();
    }

    /**
     * Removes an NPC from management and despawns it if currently spawned.
     * @param uuid The UUID of the NPC to remove.
     * @return True if the NPC was found and removed, false otherwise.
     */
    public static boolean removeNPC(UUID uuid) {
        NPC npc = npcs.remove(uuid);
        if (npc != null) {
            npc.despawn();
            return true;
        }
        return false;
    }

    /**
     * Helper to create a PlayerProfile with a specific name and UUID.
     * This profile can then be used to set a skin.
     * @param name The name for the profile.
     * @param uuid The UUID for the profile.
     * @return A new PlayerProfile.
     */
    public static PlayerProfile createPlayerProfile(String name, UUID uuid) {
        PlayerProfile profile = Bukkit.createProfile(uuid, name);
        // You would typically fetch skin data here and apply it to the profile.
        // For a basic example, we'll leave it without skin data.
        // Example: profile.getProperties().add(new Property("textures", "value", "signature"));
        return profile;
    }
}
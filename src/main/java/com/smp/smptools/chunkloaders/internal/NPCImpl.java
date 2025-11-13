package com.smp.smptools.chunkloaders.internal;

import com.smp.smptools.chunkloaders.api.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.util.Vector;

import java.util.UUID;

public class NPCImpl implements NPC {

    private final UUID uuid;
    private final String name;
    private final PlayerProfile profile;
    private Location location;
    private boolean spawned;

    private Object nmsPlayer; // NMS EntityPlayer instance

    public NPCImpl(UUID uuid, String name, PlayerProfile profile, Location location) {
        this.uuid = uuid;
        this.name = name;
        this.profile = profile;
        this.location = location;
        this.spawned = false;
    }

    @Override
    public void spawn() {
        if (spawned) return;

        Object gameProfile = PacketUtils.getGameProfile(profile);
        if (gameProfile == null) {
            System.err.println("Failed to get GameProfile for NPC " + name);
            return;
        }

        this.nmsPlayer = PacketUtils.createNMSPlayer(profile, location);
        if (this.nmsPlayer == null) {
            System.err.println("Failed to create NMS Player for NPC " + name);
            return;
        }

        Object playerInfoPacket = PacketUtils.createPlayerInfoPacket(PacketUtils.getPlayerInfoActionAddPlayer(), nmsPlayer);
        Object namedEntitySpawnPacket = PacketUtils.createNamedEntitySpawnPacket(nmsPlayer);
        Object headRotationPacket = PacketUtils.createEntityHeadRotationPacket(nmsPlayer, location.getYaw());

        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketUtils.sendPacket(player, playerInfoPacket); // Add to tab list
            PacketUtils.sendPacket(player, namedEntitySpawnPacket); // Spawn entity
            PacketUtils.sendPacket(player, headRotationPacket); // Set head rotation
        }

        // Schedule removal from tab list after a short delay to keep it from showing permanently
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugins()[0], () -> {
            Object removePlayerInfoPacket = PacketUtils.createPlayerInfoPacket(PacketUtils.getPlayerInfoActionRemovePlayer(), nmsPlayer);
            for (Player player : Bukkit.getOnlinePlayers()) {
                PacketUtils.sendPacket(player, removePlayerInfoPacket);
            }
        }, 20L); // 1 second delay

        this.spawned = true;
    }

    @Override
    public void despawn() {
        if (!spawned) return;

        int entityId = PacketUtils.getEntityId(nmsPlayer);
        Object destroyPacket = PacketUtils.createEntityDestroyPacket(entityId);
        Object removePlayerInfoPacket = PacketUtils.createPlayerInfoPacket(PacketUtils.getPlayerInfoActionRemovePlayer(), nmsPlayer);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketUtils.sendPacket(player, destroyPacket);
            PacketUtils.sendPacket(player, removePlayerInfoPacket);
        }

        this.nmsPlayer = null;
        this.spawned = false;
    }

    @Override
    public void teleport(Location newLocation) {
        if (!spawned) {
            this.location = newLocation;
            return;
        }

        this.location = newLocation;
        Object teleportPacket = PacketUtils.createEntityTeleportPacket(nmsPlayer, newLocation);
        Object headRotationPacket = PacketUtils.createEntityHeadRotationPacket(nmsPlayer, newLocation.getYaw());

        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketUtils.sendPacket(player, teleportPacket);
            PacketUtils.sendPacket(player, headRotationPacket);
        }
    }

    @Override
    public void lookAt(Location targetLocation) {
        if (!spawned) return;

        Location currentLoc = this.location.clone();
        Vector direction = targetLocation.toVector().subtract(currentLoc.toVector()).normalize();

        double x = direction.getX();
        double z = direction.getZ();

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90;
        float pitch = (float) Math.toDegrees(Math.atan2(direction.getY(), Math.sqrt(x * x + z * z)));

        // Update NPC's internal location yaw/pitch
        this.location.setYaw(yaw);
        this.location.setPitch(pitch);

        Object lookPacket = PacketUtils.createEntityLookPacket(nmsPlayer, yaw, pitch);
        Object headRotationPacket = PacketUtils.createEntityHeadRotationPacket(nmsPlayer, yaw);

        for (Player player : Bukkit.getOnlinePlayers()) {
            PacketUtils.sendPacket(player, lookPacket);
            PacketUtils.sendPacket(player, headRotationPacket);
        }
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public PlayerProfile getProfile() {
        return profile;
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }
}
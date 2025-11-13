package com.smp.smptools.chunkloaders.internal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class PacketUtils {

    private static String VERSION_STRING;

    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        VERSION_STRING = packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    // NMS Classes (loaded via reflection)
    private static Class<?> NMS_ENTITY_PLAYER_CLASS;
    private static Class<?> NMS_MINECRAFT_SERVER_CLASS;
    private static Class<?> NMS_WORLD_SERVER_CLASS;
    private static Class<?> NMS_PLAYER_INTERACTION_MANAGER_CLASS;
    private static Class<?> NMS_PLAYER_CONNECTION_CLASS;
    private static Class<?> NMS_PACKET_CLASS;
    private static Class<?> NMS_PACKET_PLAY_OUT_PLAYER_INFO_CLASS;
    private static Class<?> NMS_PACKET_PLAY_OUT_NAMED_ENTITY_SPAWN_CLASS;
    private static Class<?> NMS_PACKET_PLAY_OUT_ENTITY_HEAD_ROTATION_CLASS;
    private static Class<?> NMS_PACKET_PLAY_OUT_ENTITY_TELEPORT_CLASS;
    private static Class<?> NMS_PACKET_PLAY_OUT_ENTITY_DESTROY_CLASS;
    private static Class<?> NMS_PACKET_PLAY_OUT_ENTITY_LOOK_CLASS;
    private static Class<?> NMS_ENUM_PLAYER_INFO_ACTION_CLASS;
    private static Class<?> NMS_DATA_WATCHER_CLASS;
    private static Class<?> NMS_DATA_WATCHER_OBJECT_CLASS;
    private static Class<?> NMS_DATA_WATCHER_REGISTRY_CLASS;
    private static Class<?> NMS_ENTITY_CLASS;
    private static Class<?> GAME_PROFILE_CLASS;


    // CraftBukkit Classes (loaded via reflection)
    private static Class<?> CRAFT_PLAYER_CLASS;
    private static Class<?> CRAFT_SERVER_CLASS;
    private static Class<?> CRAFT_WORLD_CLASS;

    // Methods
    private static Method GET_HANDLE_METHOD;
    private static Method GET_PLAYER_CONNECTION_METHOD;
    private static Method SEND_PACKET_METHOD;
    private static Method GET_PROFILE_METHOD;
    private static Method GET_DATA_WATCHER_METHOD;
    private static Method GET_DATA_WATCHER_ITEM_METHOD;
    private static Method GET_DATA_WATCHER_REGISTRY_ITEM_METHOD;


    // Fields
    private static Field PLAYER_INFO_DATA_FIELD;
    private static Field PLAYER_INFO_ACTION_ADD_PLAYER_FIELD;
    private static Field PLAYER_INFO_ACTION_REMOVE_PLAYER_FIELD;
    private static Field DATA_WATCHER_FLAGS_FIELD; // For entity flags (e.g., skin parts)

    static {
        try {
            // NMS Classes
            NMS_ENTITY_PLAYER_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".EntityPlayer");
            NMS_MINECRAFT_SERVER_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".MinecraftServer");
            NMS_WORLD_SERVER_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".WorldServer");
            NMS_PLAYER_INTERACTION_MANAGER_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PlayerInteractManager");
            NMS_PLAYER_CONNECTION_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PlayerConnection");
            NMS_PACKET_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".Packet");
            NMS_PACKET_PLAY_OUT_PLAYER_INFO_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PacketPlayOutPlayerInfo");
            NMS_PACKET_PLAY_OUT_NAMED_ENTITY_SPAWN_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PacketPlayOutNamedEntitySpawn");
            NMS_PACKET_PLAY_OUT_ENTITY_HEAD_ROTATION_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PacketPlayOutEntityHeadRotation");
            NMS_PACKET_PLAY_OUT_ENTITY_TELEPORT_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PacketPlayOutEntityTeleport");
            NMS_PACKET_PLAY_OUT_ENTITY_DESTROY_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PacketPlayOutEntityDestroy");
            NMS_PACKET_PLAY_OUT_ENTITY_LOOK_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PacketPlayOutEntityLook");
            NMS_ENUM_PLAYER_INFO_ACTION_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            NMS_DATA_WATCHER_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".DataWatcher");
            NMS_DATA_WATCHER_OBJECT_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".DataWatcherObject");
            NMS_DATA_WATCHER_REGISTRY_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".DataWatcherRegistry");
            NMS_ENTITY_CLASS = Class.forName("net.minecraft.server." + VERSION_STRING + ".Entity");
            GAME_PROFILE_CLASS = Class.forName("com.mojang.authlib.GameProfile");


            // CraftBukkit Classes
            CRAFT_PLAYER_CLASS = Class.forName("org.bukkit.craftbukkit." + VERSION_STRING + ".entity.CraftPlayer");
            CRAFT_SERVER_CLASS = Class.forName("org.bukkit.craftbukkit." + VERSION_STRING + ".CraftServer");
            CRAFT_WORLD_CLASS = Class.forName("org.bukkit.craftbukkit." + VERSION_STRING + ".CraftWorld");

            // Methods
            GET_HANDLE_METHOD = CRAFT_PLAYER_CLASS.getMethod("getHandle");
            GET_PLAYER_CONNECTION_METHOD = NMS_ENTITY_PLAYER_CLASS.getField("playerConnection").getType().getMethod("sendPacket", NMS_PACKET_CLASS);
            SEND_PACKET_METHOD = NMS_PLAYER_CONNECTION_CLASS.getMethod("sendPacket", NMS_PACKET_CLASS);
            GET_PROFILE_METHOD = NMS_ENTITY_PLAYER_CLASS.getMethod("getProfile");
            GET_DATA_WATCHER_METHOD = NMS_ENTITY_CLASS.getMethod("getDataWatcher");

            // Fields for PacketPlayOutPlayerInfo
            // This field name might vary slightly between versions.
            // It's usually a static field holding the EnumPlayerInfoAction.
            // For 1.16+, it's often an enum.
            try {
                PLAYER_INFO_ACTION_ADD_PLAYER_FIELD = NMS_ENUM_PLAYER_INFO_ACTION_CLASS.getField("ADD_PLAYER");
                PLAYER_INFO_ACTION_REMOVE_PLAYER_FIELD = NMS_ENUM_PLAYER_INFO_ACTION_CLASS.getField("REMOVE_PLAYER");
            } catch (NoSuchFieldException e) {
                // Fallback for older versions or different enum structure if needed
                // For simplicity, this example assumes the above field names.
                System.err.println("Could not find EnumPlayerInfoAction fields directly. This might be an older version or different structure.");
            }

            // DataWatcher fields for entity metadata (e.g., skin parts)
            // The index for the entity flags (byte) is usually 0.
            // The DataWatcherObject for this is often a static field in Entity.
            Field dataWatcherRegistryByteField = NMS_DATA_WATCHER_REGISTRY_CLASS.getField("a"); // DataWatcherRegistry.a is usually the Byte serializer
            Object dataWatcherRegistryByte = dataWatcherRegistryByteField.get(null);
            Constructor<?> dataWatcherObjectConstructor = NMS_DATA_WATCHER_OBJECT_CLASS.getConstructor(int.class, NMS_DATA_WATCHER_REGISTRY_CLASS);
            DATA_WATCHER_FLAGS_FIELD = NMS_ENTITY_CLASS.getDeclaredField("DATA_SHARED_FLAGS"); // This field name can vary, common in 1.16+
            DATA_WATCHER_FLAGS_FIELD.setAccessible(true);
            GET_DATA_WATCHER_ITEM_METHOD = NMS_DATA_WATCHER_CLASS.getMethod("getItem", NMS_DATA_WATCHER_OBJECT_CLASS);
            GET_DATA_WATCHER_REGISTRY_ITEM_METHOD = NMS_DATA_WATCHER_CLASS.getMethod("set", NMS_DATA_WATCHER_OBJECT_CLASS, Object.class);


        } catch (Exception e) {
            System.err.println("Failed to initialize NMS reflection for FakePlayerAPI: " + e.getMessage());
            e.printStackTrace();
            // Consider disabling the API or throwing a more specific exception
        }
    }

    /**
     * Creates an NMS EntityPlayer instance. This is a server-side representation
     * of a player, necessary for sending player-related packets.
     * @param profile The GameProfile for the NPC.
     * @param location The initial location.
     * @return An NMS EntityPlayer object.
     */
    public static Object createNMSPlayer(PlayerProfile profile, Location location) {
        try {
            Object minecraftServer = CRAFT_SERVER_CLASS.getMethod("getServer").invoke(Bukkit.getServer());
            Object worldServer = CRAFT_WORLD_CLASS.getMethod("getHandle").invoke(location.getWorld());

            Constructor<?> playerInteractionManagerConstructor = NMS_PLAYER_INTERACTION_MANAGER_CLASS.getConstructor(NMS_WORLD_SERVER_CLASS);
            Object playerInteractionManager = playerInteractionManagerConstructor.newInstance(worldServer);

            Constructor<?> entityPlayerConstructor = NMS_ENTITY_PLAYER_CLASS.getConstructor(
                    NMS_MINECRAFT_SERVER_CLASS,
                    NMS_WORLD_SERVER_CLASS,
                    GAME_PROFILE_CLASS,
                    NMS_PLAYER_INTERACTION_MANAGER_CLASS
            );
            Object nmsPlayer = entityPlayerConstructor.newInstance(
                    minecraftServer,
                    worldServer,
                    getGameProfile(profile),
                    playerInteractionManager
            );

            // Set initial location
            Method setLocationMethod = NMS_ENTITY_PLAYER_CLASS.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            setLocationMethod.invoke(nmsPlayer, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            // Set skin parts (e.g., cape, jacket, etc.)
            // This is usually done by setting a byte in the DataWatcher (index 0, bitmask)
            Object dataWatcher = GET_DATA_WATCHER_METHOD.invoke(nmsPlayer);
            Object dataWatcherObjectFlags = DATA_WATCHER_FLAGS_FIELD.get(null); // Get the static DataWatcherObject for flags
            // The byte value for all skin parts enabled is 0x7F (127)
            GET_DATA_WATCHER_REGISTRY_ITEM_METHOD.invoke(dataWatcher, dataWatcherObjectFlags, (byte) 0x7F);


            return nmsPlayer;

        } catch (Exception e) {
            System.err.println("Failed to create NMS EntityPlayer: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Sends an NMS packet to a specific player.
     * @param player The Bukkit player to send the packet to.
     * @param packet The NMS packet object.
     */
    public static void sendPacket(Player player, Object packet) {
        try {
            Object craftPlayer = CRAFT_PLAYER_CLASS.cast(player);
            Object nmsPlayer = GET_HANDLE_METHOD.invoke(craftPlayer);
            Object playerConnection = NMS_ENTITY_PLAYER_CLASS.getField("playerConnection").get(nmsPlayer);
            SEND_PACKET_METHOD.invoke(playerConnection, packet);
        } catch (Exception e) {
            System.err.println("Failed to send packet to player " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a PacketPlayOutPlayerInfo packet.
     * @param action The action (ADD_PLAYER or REMOVE_PLAYER).
     * @param nmsPlayer The NMS EntityPlayer object.
     * @return The PacketPlayOutPlayerInfo packet.
     */
    public static Object createPlayerInfoPacket(Object action, Object nmsPlayer) {
        try {
            Constructor<?> constructor = NMS_PACKET_PLAY_OUT_PLAYER_INFO_CLASS.getConstructor(NMS_ENUM_PLAYER_INFO_ACTION_CLASS, NMS_ENTITY_PLAYER_CLASS.arrayType());
            return constructor.newInstance(action, new Object[]{nmsPlayer});
        } catch (Exception e) {
            System.err.println("Failed to create PacketPlayOutPlayerInfo: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a PacketPlayOutNamedEntitySpawn packet.
     * @param nmsPlayer The NMS EntityPlayer object.
     * @return The PacketPlayOutNamedEntitySpawn packet.
     */
    public static Object createNamedEntitySpawnPacket(Object nmsPlayer) {
        try {
            Constructor<?> constructor = NMS_PACKET_PLAY_OUT_NAMED_ENTITY_SPAWN_CLASS.getConstructor(NMS_ENTITY_PLAYER_CLASS);
            return constructor.newInstance(nmsPlayer);
        } catch (Exception e) {
            System.err.println("Failed to create PacketPlayOutNamedEntitySpawn: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a PacketPlayOutEntityHeadRotation packet.
     * @param nmsPlayer The NMS EntityPlayer object.
     * @param yaw The head rotation yaw.
     * @return The PacketPlayOutEntityHeadRotation packet.
     */
    public static Object createEntityHeadRotationPacket(Object nmsPlayer, float yaw) {
        try {
            // Entity ID is usually an int field in EntityPlayer (inherited from Entity)
            Field entityIdField = NMS_ENTITY_PLAYER_CLASS.getSuperclass().getDeclaredField("id");
            entityIdField.setAccessible(true);
            int entityId = (int) entityIdField.get(nmsPlayer);

            Constructor<?> constructor = NMS_PACKET_PLAY_OUT_ENTITY_HEAD_ROTATION_CLASS.getConstructor(int.class, byte.class);
            return constructor.newInstance(entityId, (byte) ((yaw * 256.0F) / 360.0F));
        } catch (Exception e) {
            System.err.println("Failed to create PacketPlayOutEntityHeadRotation: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a PacketPlayOutEntityTeleport packet.
     * @param nmsPlayer The NMS EntityPlayer object.
     * @param location The new location.
     * @return The PacketPlayOutEntityTeleport packet.
     */
    public static Object createEntityTeleportPacket(Object nmsPlayer, Location location) {
        try {
            // Entity ID is usually an int field in EntityPlayer (inherited from Entity)
            Field entityIdField = NMS_ENTITY_PLAYER_CLASS.getSuperclass().getDeclaredField("id");
            entityIdField.setAccessible(true);
            int entityId = (int) entityIdField.get(nmsPlayer);

            Constructor<?> constructor = NMS_PACKET_PLAY_OUT_ENTITY_TELEPORT_CLASS.getConstructor(int.class, double.class, double.class, double.class, byte.class, byte.class, boolean.class);
            return constructor.newInstance(
                    entityId,
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    (byte) ((location.getYaw() * 256.0F) / 360.0F),
                    (byte) ((location.getPitch() * 256.0F) / 360.0F),
                    false // onGround
            );
        } catch (Exception e) {
            System.err.println("Failed to create PacketPlayOutEntityTeleport: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a PacketPlayOutEntityDestroy packet.
     * @param entityId The entity ID to destroy.
     * @return The PacketPlayOutEntityDestroy packet.
     */
    public static Object createEntityDestroyPacket(int entityId) {
        try {
            Constructor<?> constructor = NMS_PACKET_PLAY_OUT_ENTITY_DESTROY_CLASS.getConstructor(int[].class);
            return constructor.newInstance(new int[]{entityId});
        } catch (Exception e) {
            System.err.println("Failed to create PacketPlayOutEntityDestroy: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a PacketPlayOutEntityLook packet.
     * @param nmsPlayer The NMS EntityPlayer object.
     * @param yaw The new yaw.
     * @param pitch The new pitch.
     * @return The PacketPlayOutEntityLook packet.
     */
    public static Object createEntityLookPacket(Object nmsPlayer, float yaw, float pitch) {
        try {
            // Entity ID is usually an int field in EntityPlayer (inherited from Entity)
            Field entityIdField = NMS_ENTITY_PLAYER_CLASS.getSuperclass().getDeclaredField("id");
            entityIdField.setAccessible(true);
            int entityId = (int) entityIdField.get(nmsPlayer);

            Constructor<?> constructor = NMS_PACKET_PLAY_OUT_ENTITY_LOOK_CLASS.getConstructor(int.class, byte.class, byte.class, boolean.class);
            return constructor.newInstance(
                    entityId,
                    (byte) ((yaw * 256.0F) / 360.0F),
                    (byte) ((pitch * 256.0F) / 360.0F),
                    false // onGround
            );
        } catch (Exception e) {
            System.err.println("Failed to create PacketPlayOutEntityLook: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the GameProfile from a Bukkit PlayerProfile.
     * @param playerProfile The Bukkit PlayerProfile.
     * @return The corresponding GameProfile.
     */
    public static Object getGameProfile(PlayerProfile playerProfile) {
        try {
            Method getGameProfileMethod = playerProfile.getClass().getMethod("getGameProfile");
            return getGameProfileMethod.invoke(playerProfile);
        } catch (Exception e) {
            System.err.println("Failed to get GameProfile from PlayerProfile: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the NMS EnumPlayerInfoAction.ADD_PLAYER enum constant.
     * @return The ADD_PLAYER enum constant.
     */
    public static Object getPlayerInfoActionAddPlayer() {
        try {
            return PLAYER_INFO_ACTION_ADD_PLAYER_FIELD.get(null);
        } catch (Exception e) {
            System.err.println("Failed to get ADD_PLAYER action: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the NMS EnumPlayerInfoAction.REMOVE_PLAYER enum constant.
     * @return The REMOVE_PLAYER enum constant.
     */
    public static Object getPlayerInfoActionRemovePlayer() {
        try {
            return PLAYER_INFO_ACTION_REMOVE_PLAYER_FIELD.get(null);
        } catch (Exception e) {
            System.err.println("Failed to get REMOVE_PLAYER action: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the entity ID from an NMS EntityPlayer object.
     * @param nmsPlayer The NMS EntityPlayer.
     * @return The entity ID.
     */
    public static int getEntityId(Object nmsPlayer) {
        try {
            Field entityIdField = NMS_ENTITY_PLAYER_CLASS.getSuperclass().getDeclaredField("id");
            entityIdField.setAccessible(true);
            return (int) entityIdField.get(nmsPlayer);
        } catch (Exception e) {
            System.err.println("Failed to get entity ID from NMS Player: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
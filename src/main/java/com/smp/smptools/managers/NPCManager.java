package com.smp.smptools.managers;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class NPCManager {

    private final SMPTools plugin;
    private File npcsFile;
    private FileConfiguration npcsConfig;
    private final Map<String, Entity> npcRegistry = new ConcurrentHashMap<>();

    public static final NamespacedKey STORY_NPC_KEY = new NamespacedKey("smptools", "story_npc");
    public static final NamespacedKey SANTA_NPC_KEY = new NamespacedKey("smptools", "santa_npc");
    public static final NamespacedKey MISSION_NPC_KEY = new NamespacedKey("smptools", "mission_npc");
    public static final NamespacedKey NPC_ID_KEY = new NamespacedKey("smptools", "npc_id");
    public static final NamespacedKey DIALOGUE_ID_KEY = new NamespacedKey("smptools", "dialogue_id");

    public NPCManager(SMPTools plugin) {
        this.plugin = plugin;
        loadConfig();
        setupTeam();
    }

    private void loadConfig() {
        npcsFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!npcsFile.exists()) {
            plugin.saveResource("npcs.yml", false);
        }
        npcsConfig = YamlConfiguration.loadConfiguration(npcsFile);
    }

    public void saveConfig() {
        try {
            com.smp.smptools.utils.AtomicFileWriter.save(npcsConfig, npcsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save npcs.yml", e);
        }
    }

    public void loadNPCs() {
        ConfigurationSection npcs = npcsConfig.getConfigurationSection("npcs");
        if (npcs == null)
            return;

        for (String id : npcs.getKeys(false)) {
            spawnNPC(id);
        }
    }

    public void spawnNPC(String id) {
        ConfigurationSection section = npcsConfig.getConfigurationSection("npcs." + id);
        if (section == null)
            return;

        Location loc = getLocationFromConfig(section.getConfigurationSection("location"));
        if (loc == null) {
            plugin.getLogger().warning("Invalid location for NPC " + id);
            return;
        }

        String type = section.getString("type", "DIALOGUE");
        String skinName = section.getString("skin", "Steve");
        String name = section.getString("name"); // Configurable name
        String dialogueId = section.getString("dialogue-id");

        // Force load chunk to ensure we can find existing entities
        if (!loc.getChunk().isLoaded()) {
            loc.getChunk().load();
        }

        // Remove existing NPC with this ID at this location (increased radius to ensure
        // cleanup)
        loc.getNearbyEntities(3, 3, 3).forEach(entity -> {
            if (entity.getPersistentDataContainer().has(NPC_ID_KEY, PersistentDataType.STRING) &&
                    entity.getPersistentDataContainer().get(NPC_ID_KEY, PersistentDataType.STRING).equals(id)) {
                entity.remove();
            }
        });

        EntityType entityType;
        try {
            entityType = EntityType.valueOf("MANNEQUIN");
        } catch (IllegalArgumentException e) {
            // Fallback for older versions or if Mannequin isn't available
            spawnFallbackNPC(id, loc, type, skinName, name, dialogueId);
            return;
        }

        Entity npc = loc.getWorld().spawnEntity(loc, entityType);
        npc.setCustomNameVisible(true);
        if (name != null && !name.isEmpty()) {
            npc.customName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name));
        }
        npc.getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, id);
        if (dialogueId != null) {
            npc.getPersistentDataContainer().set(DIALOGUE_ID_KEY, PersistentDataType.STRING, dialogueId);
        }

        // Set specific keys for Mission/Santa NPCs to enable GUI interaction
        if ("SANTA".equalsIgnoreCase(type)) {
            npc.getPersistentDataContainer().set(SANTA_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        } else if ("MISSIONS".equalsIgnoreCase(type)) {
            npc.getPersistentDataContainer().set(MISSION_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        } else {
            npc.getPersistentDataContainer().set(STORY_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        }

        if (npc instanceof LivingEntity) {
            ((LivingEntity) npc).setAI(false);
            ((LivingEntity) npc).setCollidable(false);
            ((LivingEntity) npc).setInvulnerable(true);
            ((LivingEntity) npc).setSilent(true);
            ((LivingEntity) npc).setGravity(false);
        }

        // Apply Skin
        if (npc instanceof Player) {
            try {
                PlayerProfile profile = Bukkit.createProfile(skinName);
                if (!profile.complete(false)) {
                    profile.complete(true);
                }
                ((Player) npc).setPlayerProfile(profile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load skin for NPC " + id + ": " + e.getMessage());
            }
        } else if (entityType.name().equals("MANNEQUIN")) {
            // Use /data merge for Mannequin skin
            String command = String.format("data merge entity %s {profile:{name:\"%s\"}}", npc.getUniqueId(), skinName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else {
            plugin.getLogger().info(
                    "NPC " + id + " is not a Player or Mannequin (" + npc.getType() + "), skipping skin application.");
        }
        addEntityToTeam(npc);
        npcRegistry.put(id, npc);
    }

    public void createNPC(String id, Location loc, String type, String skin, String name) {
        ConfigurationSection section = npcsConfig.createSection("npcs." + id);
        saveLocationToConfig(section.createSection("location"), loc);
        section.set("type", type);
        section.set("skin", skin);
        if (name != null)
            section.set("name", name);
        saveConfig();
        spawnNPC(id);
    }

    public void removeNPC(String id) {
        npcsConfig.set("npcs." + id, null);
        saveConfig();

        Entity npc = npcRegistry.remove(id);
        if (npc != null && !npc.isDead()) {
            npc.remove();
        }
    }

    public void removeAllNPCs() {
        ConfigurationSection npcs = npcsConfig.getConfigurationSection("npcs");
        if (npcs == null)
            return;

        for (String id : npcs.getKeys(false)) {
            ConfigurationSection section = npcsConfig.getConfigurationSection("npcs." + id);
            if (section == null)
                continue;

            Location loc = getLocationFromConfig(section.getConfigurationSection("location"));
            if (loc == null)
                continue;

            if (!loc.getChunk().isLoaded()) {
                loc.getChunk().load();
            }

            loc.getNearbyEntities(3, 3, 3).forEach(entity -> {
                if (entity.getPersistentDataContainer().has(NPC_ID_KEY, PersistentDataType.STRING) &&
                        entity.getPersistentDataContainer().get(NPC_ID_KEY, PersistentDataType.STRING).equals(id)) {
                    entity.remove();
                }
            });
        }
    }

    private void setupTeam() {
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = scoreboard.getTeam("npc_team");
        if (team == null) {
            team = scoreboard.registerNewTeam("npc_team");
        }
        team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
    }

    private void addEntityToTeam(Entity entity) {
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        org.bukkit.scoreboard.Team team = scoreboard.getTeam("npc_team");
        if (team != null) {
            team.addEntry(entity.getUniqueId().toString());
        }
    }

    private void spawnFallbackNPC(String id, Location loc, String type, String skinName, String name,
            String dialogueId) {
        if ("SANTA".equalsIgnoreCase(type)) {
            Zombie z = loc.getWorld().spawn(loc, Zombie.class);
            z.setAI(false);
            z.setInvulnerable(true);
            z.setSilent(true);
            z.setBaby(false);
            z.setShouldBurnInDay(false);
            z.setCollidable(false);
            z.setGravity(false);
            z.setCustomNameVisible(true);
            if (name != null && !name.isEmpty()) {
                z.customName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name));
            } else {
                z.customName(Component.text("Santa Claus", NamedTextColor.RED));
            }
            z.getPersistentDataContainer().set(SANTA_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
            z.getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, id);
            if (dialogueId != null) {
                z.getPersistentDataContainer().set(DIALOGUE_ID_KEY, PersistentDataType.STRING, dialogueId);
            }

            if (skinName != null && !skinName.isEmpty()) {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
                PlayerProfile profile = Bukkit.createProfile(skinName);
                profile.complete(true);
                meta.setPlayerProfile(profile);
                head.setItemMeta(meta);
                z.getEquipment().setHelmet(head);
            }
            addEntityToTeam(z);
        } else {
            Villager v = loc.getWorld().spawn(loc, Villager.class);
            v.setAI(false);
            v.setInvulnerable(true);
            v.setSilent(true);
            v.setCollidable(false);
            v.setGravity(false);
            v.setCustomNameVisible(true);
            if (name != null && !name.isEmpty()) {
                v.customName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name));
            } else {
                v.customName(Component.text("NPC", NamedTextColor.GREEN));
                if ("MISSIONS".equalsIgnoreCase(type)) {
                    v.customName(Component.text("Quest Master", NamedTextColor.GOLD));
                }
            }
            if ("MISSIONS".equalsIgnoreCase(type)) {
                v.getPersistentDataContainer().set(MISSION_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
            } else {
                v.getPersistentDataContainer().set(STORY_NPC_KEY, PersistentDataType.BYTE, (byte) 1);
            }
            v.getPersistentDataContainer().set(NPC_ID_KEY, PersistentDataType.STRING, id);
            if (dialogueId != null) {
                v.getPersistentDataContainer().set(DIALOGUE_ID_KEY, PersistentDataType.STRING, dialogueId);
            }
            addEntityToTeam(v);
        }
    }

    public void removeAllNPCs(Player player, int radius) {
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity.getPersistentDataContainer().has(NPC_ID_KEY, PersistentDataType.STRING) ||
                    entity.getPersistentDataContainer().has(STORY_NPC_KEY, PersistentDataType.BYTE) ||
                    entity.getPersistentDataContainer().has(SANTA_NPC_KEY, PersistentDataType.BYTE) ||
                    entity.getPersistentDataContainer().has(MISSION_NPC_KEY, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }

    private Location getLocationFromConfig(ConfigurationSection section) {
        if (section == null)
            return null;
        World world = Bukkit.getWorld(section.getString("world"));
        if (world == null)
            return null;
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("yaw"), (float) section.getDouble("pitch"));
    }

    private void saveLocationToConfig(ConfigurationSection section, Location loc) {
        section.set("world", loc.getWorld().getName());
        section.set("x", loc.getX());
        section.set("y", loc.getY());
        section.set("z", loc.getZ());
        section.set("yaw", loc.getYaw());
        section.set("pitch", loc.getPitch());
    }
}

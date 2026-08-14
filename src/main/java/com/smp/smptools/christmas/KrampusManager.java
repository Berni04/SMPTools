package com.smp.smptools.christmas;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KrampusManager {

    private final SMPTools plugin;
    private FileConfiguration christmasConfig;
    private final Map<UUID, Location> kidnappedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Location> playerCages = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> playerGuards = new ConcurrentHashMap<>();
    public final NamespacedKey krampusKey;

    public KrampusManager(SMPTools plugin) {
        this.plugin = plugin;
        this.krampusKey = new NamespacedKey(plugin, "krampus_entity");
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "christmas.yml");
        if (file.exists()) {
            christmasConfig = YamlConfiguration.loadConfiguration(file);
        }
    }

    public void spawnKrampus(Location location) {
        WitherSkeleton krampus = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);

        // Stats
        double health = christmasConfig.getDouble("krampus.health", 300.0);
        krampus.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
        krampus.setHealth(health);

        // Name
        String name = christmasConfig.getString("krampus.name", "&c&lKrampus");
        krampus.customName(LegacyComponentSerializer.legacySection().deserialize(name.replace("&", "§")));
        krampus.setCustomNameVisible(true);

        // Equipment
        krampus.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        krampus.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        krampus.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));

        // Persistent Data
        krampus.getPersistentDataContainer().set(krampusKey, PersistentDataType.BYTE, (byte) 1);
    }

    public void kidnapPlayer(Player player, WitherSkeleton krampus) {
        if (kidnappedPlayers.containsKey(player.getUniqueId()))
            return;

        // Despawn Krampus
        if (krampus != null) {
            krampus.remove();
        }

        // Save location
        kidnappedPlayers.put(player.getUniqueId(), player.getLocation());

        // Cage Location (High up)
        Location cageLoc = player.getLocation().clone().add(0, 50, 0);
        playerCages.put(player.getUniqueId(), cageLoc);

        // Build Cage
        buildCage(cageLoc);

        // Teleport
        player.teleport(cageLoc.clone().add(0.5, 1, 0.5));
        player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("krampus.kidnapped"));

        // Spawn Guards
        Set<UUID> guards = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Zombie guard = (Zombie) cageLoc.getWorld().spawnEntity(cageLoc.clone().add(0.5, 1, 0.5), EntityType.ZOMBIE);
            guard.customName(Component.text("Cage Guard", NamedTextColor.RED));
            guard.setCustomNameVisible(true);
            guard.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
            guards.add(guard.getUniqueId());
        }
        playerGuards.put(player.getUniqueId(), guards);
    }

    public void checkGuardDeath(Player player, UUID guardId) {
        if (playerGuards.containsKey(player.getUniqueId())) {
            Set<UUID> guards = playerGuards.get(player.getUniqueId());
            if (guards.remove(guardId)) {
                if (guards.isEmpty()) {
                    releasePlayer(player);
                    playerGuards.remove(player.getUniqueId());
                } else {
                    player.sendMessage(
                            SMPTools.getInstance().getMessageManager().getMessage("krampus.guard-defeated").replaceText(builder -> builder.matchLiteral("{remaining}").replacement(String.valueOf(guards.size()))));
                }
            }
        }
    }

    public void releasePlayer(Player player) {
        if (player == null) return;
        releasePlayer(player.getUniqueId());
    }

    public void releasePlayer(UUID uuid) {
        if (!kidnappedPlayers.containsKey(uuid) && !playerCages.containsKey(uuid))
            return;

        Location cageLoc = playerCages.remove(uuid);
        Location originalLoc = kidnappedPlayers.remove(uuid);
        despawnGuards(uuid);

        if (cageLoc != null) {
            removeCage(cageLoc);
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && originalLoc != null) {
            player.teleport(originalLoc);
            player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage("krampus.escaped"));
        }
    }

    private void despawnGuards(UUID uuid) {
        Set<UUID> guards = playerGuards.remove(uuid);
        if (guards != null) {
            for (UUID guardId : guards) {
                org.bukkit.entity.Entity entity = Bukkit.getEntity(guardId);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
    }

    private void removeCage(Location center) {
        if (center == null || center.getWorld() == null) return;
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = -4; z <= 4; z++) {
                    org.bukkit.block.Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType() == Material.IRON_BARS || block.getType() == Material.BEDROCK) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    public void cleanupAll() {
        for (UUID uuid : new HashSet<>(playerCages.keySet())) {
            releasePlayer(uuid);
        }
    }

    private void buildCage(Location center) {
        // 9x9 Cage (Radius 4)
        for (int x = -4; x <= 4; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (x == -4 || x == 4 || z == -4 || z == 4 || y == 0 || y == 5) {
                        center.clone().add(x, y, z).getBlock().setType(Material.IRON_BARS);
                    } else {
                        center.clone().add(x, y, z).getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        // Floor
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                center.clone().add(x, 0, z).getBlock().setType(Material.BEDROCK);
            }
        }
    }

    public boolean isKidnapped(Player player) {
        return player != null && kidnappedPlayers.containsKey(player.getUniqueId());
    }
}

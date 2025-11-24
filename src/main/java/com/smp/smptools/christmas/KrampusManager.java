package com.smp.smptools.christmas;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KrampusManager {

    private final SMPTools plugin;
    private FileConfiguration christmasConfig;
    private final Map<UUID, Location> kidnappedPlayers = new HashMap<>();

    public KrampusManager(SMPTools plugin) {
        this.plugin = plugin;
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
        krampus.customName(Component.text(name.replace("&", "§"))); // Simple color code support
        krampus.setCustomNameVisible(true);

        // Equipment
        krampus.getEquipment().setHelmet(new ItemStack(Material.WITHER_SKELETON_SKULL));
        krampus.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        krampus.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
    }

    public void kidnapPlayer(Player player) {
        if (kidnappedPlayers.containsKey(player.getUniqueId()))
            return;

        // Save location
        kidnappedPlayers.put(player.getUniqueId(), player.getLocation());

        // Cage Location (High up)
        Location cageLoc = player.getLocation().clone().add(0, 50, 0);

        // Build Cage
        buildCage(cageLoc);

        // Teleport
        player.teleport(cageLoc.clone().add(0.5, 1, 0.5));
        player.sendMessage(
                Component.text("You have been kidnapped by Krampus! Defeat the guard to escape!", NamedTextColor.RED));

        // Spawn Guard
        Zombie guard = (Zombie) cageLoc.getWorld().spawnEntity(cageLoc.clone().add(0.5, 1, 0.5), EntityType.ZOMBIE);
        guard.customName(Component.text("Cage Guard", NamedTextColor.RED));
        guard.setCustomNameVisible(true);
        guard.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
    }

    public void releasePlayer(Player player) {
        if (!kidnappedPlayers.containsKey(player.getUniqueId()))
            return;

        Location originalLoc = kidnappedPlayers.remove(player.getUniqueId());
        player.teleport(originalLoc);
        player.sendMessage(Component.text("You have escaped Krampus's cage!", NamedTextColor.GREEN));

        // Cleanup cage (optional, simple removal)
        Location cageLoc = player.getLocation().clone().add(0, 50, 0); // Logic needs to track cage loc if moving, but
                                                                       // simple relative works for now if static
        // For now, we won't auto-remove to avoid complex block tracking, or we could
        // just remove the glass around the player's previous pos
    }

    private void buildCage(Location center) {
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x == -2 || x == 2 || z == -2 || z == 2 || y == 0 || y == 4) {
                        center.clone().add(x, y, z).getBlock().setType(Material.IRON_BARS);
                    } else {
                        center.clone().add(x, y, z).getBlock().setType(Material.AIR);
                    }
                }
            }
        }
        // Floor
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                center.clone().add(x, 0, z).getBlock().setType(Material.BEDROCK);
            }
        }
    }

    public boolean isKidnapped(Player player) {
        return kidnappedPlayers.containsKey(player.getUniqueId());
    }
}

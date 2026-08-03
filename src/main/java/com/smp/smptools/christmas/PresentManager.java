package com.smp.smptools.christmas;

import com.smp.smptools.SMPTools;
import com.smp.smptools.missions.RewardManager;
import com.smp.smptools.utils.HeadUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class PresentManager {

    private final SMPTools plugin;
    private File presentsFile;
    private FileConfiguration presentsConfig;
    private File christmasFile;
    private FileConfiguration christmasConfig;
    private final Random random = new Random();
    public static final NamespacedKey PRESENT_TIER_KEY = new NamespacedKey(SMPTools.getInstance(), "present_tier");

    public PresentManager(SMPTools plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        presentsFile = new File(plugin.getDataFolder(), "presents.yml");
        if (!presentsFile.exists()) {
            try {
                presentsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create presents.yml!");
            }
        }
        presentsConfig = YamlConfiguration.loadConfiguration(presentsFile);

        christmasFile = new File(plugin.getDataFolder(), "christmas.yml");
        if (!christmasFile.exists()) {
            plugin.saveResource("christmas.yml", false);
        }
        christmasConfig = YamlConfiguration.loadConfiguration(christmasFile);
    }

    public void savePresentsConfig() {
        try {
            presentsConfig.save(presentsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save presents.yml!");
        }
    }

    public ItemStack getPresentItem(String tier) {
        if (!christmasConfig.contains("presents." + tier)) {
            return null;
        }

        String texture = christmasConfig.getString("presents." + tier + ".texture");
        ItemStack head = HeadUtils.getCustomHead(texture);
        ItemMeta meta = head.getItemMeta();

        String displayName = tier.substring(0, 1).toUpperCase() + tier.substring(1) + " Present";
        NamedTextColor color = NamedTextColor.WHITE;
        if (tier.equalsIgnoreCase("common"))
            color = NamedTextColor.GREEN;
        else if (tier.equalsIgnoreCase("rare"))
            color = NamedTextColor.BLUE;
        else if (tier.equalsIgnoreCase("legendary"))
            color = NamedTextColor.GOLD;

        meta.displayName(Component.text(displayName, color));
        meta.getPersistentDataContainer().set(PRESENT_TIER_KEY, PersistentDataType.STRING, tier);
        head.setItemMeta(meta);

        return head;
    }

    public void createPresent(Location location, String tier) {
        String id = UUID.randomUUID().toString();
        String path = "presents." + id;
        presentsConfig.set(path + ".world", location.getWorld().getName());
        presentsConfig.set(path + ".x", location.getBlockX());
        presentsConfig.set(path + ".y", location.getBlockY());
        presentsConfig.set(path + ".z", location.getBlockZ());
        presentsConfig.set(path + ".tier", tier);
        savePresentsConfig();
    }

    public void removePresent(Location location) {
        String id = getPresentIdAt(location);
        if (id != null) {
            presentsConfig.set("presents." + id, null);
            savePresentsConfig();
        }
    }

    public String getPresentIdAt(Location location) {
        if (presentsConfig.getConfigurationSection("presents") == null)
            return null;

        for (String id : presentsConfig.getConfigurationSection("presents").getKeys(false)) {
            String world = presentsConfig.getString("presents." + id + ".world");
            int x = presentsConfig.getInt("presents." + id + ".x");
            int y = presentsConfig.getInt("presents." + id + ".y");
            int z = presentsConfig.getInt("presents." + id + ".z");

            if (location.getWorld().getName().equals(world) &&
                    location.getBlockX() == x &&
                    location.getBlockY() == y &&
                    location.getBlockZ() == z) {
                return id;
            }
        }
        return null;
    }

    public void claimPresent(Player player, Location location) {
        String id = getPresentIdAt(location);
        if (id == null)
            return;

        String storedTier = presentsConfig.getString("presents." + id + ".tier");
        // Validate the stored tier against christmas.yml. If it is null,
        // empty, or refers to a tier that no longer exists, treat the present
        // as invalid WITHOUT overwriting the stored value with a sentinel
        // string (which would collide with a real tier named "unknown").
        boolean tierValid = storedTier != null
                && !storedTier.isEmpty()
                && christmasConfig.contains("presents." + storedTier);

        if (tierValid) {
            List<String> rewards = christmasConfig.getStringList("presents." + storedTier + ".rewards");
            for (String reward : rewards) {
                RewardManager.giveReward(player, reward);
            }
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("tier", storedTier != null ? storedTier : "unknown");
        String messageKey = tierValid ? "present.found" : "present.invalid-tier";
        player.sendMessage(SMPTools.getInstance().getMessageManager().getMessage(messageKey, player, placeholders));

        // Remove the block
        location.getBlock().setType(Material.AIR);

        // Remove from config
        presentsConfig.set("presents." + id, null);
        savePresentsConfig();
    }

    public Set<String> getTiers() {
        if (christmasConfig.getConfigurationSection("presents") == null)
            return Set.of();
        return christmasConfig.getConfigurationSection("presents").getKeys(false);
    }
}

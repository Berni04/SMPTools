package com.smp.smptools.christmas;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SecretSantaManager {

    private final SMPTools plugin;
    private File configFile;
    private FileConfiguration config;

    private static final ZoneId CET = ZoneId.of("Europe/Paris");

    public SecretSantaManager(SMPTools plugin) {
        this.plugin = plugin;
        if (plugin != null) {
            loadConfig();
        } else {
            this.config = new YamlConfiguration();
        }
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "secretsanta.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create secretsanta.yml!");
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public synchronized boolean saveConfig() {
        if (configFile == null || config == null) return true;
        try {
            config.save(configFile);
            return true;
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().severe("Could not save secretsanta.yml: " + e.getMessage());
            }
            return false;
        }
    }

    public enum Phase {
        REGISTRATION,
        PREPARATION,
        CELEBRATION
    }

    public Phase getCurrentPhase() {
        ZonedDateTime now = ZonedDateTime.now(CET);
        int day = now.getDayOfMonth();
        int month = now.getMonthValue();

        if (month != 12)
            return Phase.REGISTRATION; // Default/Fallback

        if (day <= 15)
            return Phase.REGISTRATION;
        if (day <= 24)
            return Phase.PREPARATION;
        return Phase.CELEBRATION;
    }

    public synchronized boolean isRegistered(UUID player) {
        if (player == null) return false;
        List<String> participants = config.getStringList("participants");
        return participants.contains(player.toString());
    }

    public synchronized void registerPlayer(UUID player) {
        if (player == null) return;
        List<String> participants = config.getStringList("participants");
        if (!participants.contains(player.toString())) {
            participants.add(player.toString());
            config.set("participants", participants);
            saveConfig();
        }
    }

    public synchronized void generateMatches() {
        List<String> participants = config.getStringList("participants");
        if (participants.size() < 2)
            return;

        List<String> receivers = new ArrayList<>(participants);
        Collections.shuffle(receivers);

        // Ensure no one is their own Santa (Derangement)
        boolean valid = false;
        while (!valid) {
            valid = true;
            for (int i = 0; i < participants.size(); i++) {
                if (participants.get(i).equals(receivers.get(i))) {
                    valid = false;
                    Collections.shuffle(receivers);
                    break;
                }
            }
        }

        for (int i = 0; i < participants.size(); i++) {
            config.set("matches." + participants.get(i), receivers.get(i));
        }
        saveConfig();
    }

    public synchronized UUID getTarget(UUID santa) {
        if (santa == null) return null;
        String targetStr = config.getString("matches." + santa.toString());
        return targetStr != null ? UUID.fromString(targetStr) : null;
    }

    public synchronized boolean depositGift(UUID target, ItemStack[] items) {
        if (target == null) return false;
        String path = "gifts." + target.toString();
        Object previous = config.get(path);

        List<ItemStack> list = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    list.add(item);
                }
            }
        }
        if (list.isEmpty()) {
            config.set(path, null);
        } else {
            config.set(path, list);
        }

        if (!saveConfig()) {
            config.set(path, previous);
            return false;
        }
        return true;
    }

    private static class DeserializedGiftResult {
        final List<ItemStack> validItems;
        final boolean hasMalformedItems;

        DeserializedGiftResult(List<ItemStack> validItems, boolean hasMalformedItems) {
            this.validItems = validItems;
            this.hasMalformedItems = hasMalformedItems;
        }
    }

    private DeserializedGiftResult parseGiftItems(UUID recipient, List<?> rawList) {
        List<ItemStack> items = new ArrayList<>();
        boolean hasMalformed = false;
        for (Object obj : rawList) {
            if (obj instanceof ItemStack is) {
                items.add(is);
            } else if (obj instanceof java.util.Map<?, ?> map) {
                try {
                    items.add(ItemStack.deserialize((java.util.Map<String, Object>) map));
                } catch (Exception e) {
                    hasMalformed = true;
                    if (plugin != null) {
                        plugin.getLogger().warning("Failed to deserialize Secret Santa gift item for recipient " + recipient + ": " + e.getMessage());
                    }
                }
            } else {
                hasMalformed = true;
            }
        }
        return new DeserializedGiftResult(items, hasMalformed);
    }

    public synchronized ItemStack[] getGift(UUID recipient) {
        if (recipient == null) return null;
        List<?> list = config.getList("gifts." + recipient.toString());
        if (list == null || list.isEmpty())
            return null;

        DeserializedGiftResult result = parseGiftItems(recipient, list);
        return result.validItems.isEmpty() ? null : result.validItems.toArray(new ItemStack[0]);
    }

    public synchronized ItemStack[] claimGift(UUID recipient) {
        if (recipient == null) return null;
        String path = "gifts." + recipient.toString();
        List<?> rawList = config.getList(path);
        if (rawList == null || rawList.isEmpty()) {
            return null;
        }

        DeserializedGiftResult result = parseGiftItems(recipient, rawList);
        if (result.validItems.isEmpty()) {
            // No valid items found; clear corrupted entry to avoid permanent blockage
            Object rawBackup = config.get(path);
            config.set(path, null);
            if (!saveConfig()) {
                config.set(path, rawBackup);
            }
            if (plugin != null) {
                plugin.getLogger().warning("Cleared corrupted empty gift entry for recipient " + recipient);
            }
            return null;
        }

        if (result.hasMalformedItems && plugin != null) {
            String quarantinePath = "quarantine.gifts." + recipient.toString() + "." + System.currentTimeMillis();
            config.set(quarantinePath, rawList);
            plugin.getLogger().warning("Delivering partially recoverable gift to recipient " + recipient + " while quarantining malformed entries under " + quarantinePath);
        }

        Object rawBackup = config.get(path);
        config.set(path, null);
        if (!saveConfig()) {
            config.set(path, rawBackup);
            if (plugin != null) {
                plugin.getLogger().severe("Failed to persist gift claim for recipient " + recipient + "; aborting claim.");
            }
            return null;
        }

        return result.validItems.toArray(new ItemStack[0]);
    }

    public synchronized boolean hasGiftDeposited(UUID target) {
        if (target == null) return false;
        List<?> list = config.getList("gifts." + target.toString());
        return list != null && !list.isEmpty();
    }
}

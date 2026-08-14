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

    public void saveConfig() {
        if (configFile == null || config == null) return;
        try {
            config.save(configFile);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().severe("Could not save secretsanta.yml!");
            }
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

    public boolean isRegistered(UUID player) {
        List<String> participants = config.getStringList("participants");
        return participants.contains(player.toString());
    }

    public void registerPlayer(UUID player) {
        List<String> participants = config.getStringList("participants");
        if (!participants.contains(player.toString())) {
            participants.add(player.toString());
            config.set("participants", participants);
            saveConfig();
        }
    }

    public void generateMatches() {
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

    public UUID getTarget(UUID santa) {
        String targetStr = config.getString("matches." + santa.toString());
        return targetStr != null ? UUID.fromString(targetStr) : null;
    }

    public void depositGift(UUID target, ItemStack[] items) {
        List<ItemStack> list = new ArrayList<>();
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    list.add(item);
                }
            }
        }
        config.set("gifts." + target.toString(), list);
        saveConfig();
    }

    public ItemStack[] getGift(UUID recipient) {
        List<?> list = config.getList("gifts." + recipient.toString());
        if (list == null)
            return null;

        List<ItemStack> items = new ArrayList<>();
        for (Object obj : list) {
            if (obj instanceof ItemStack is) {
                items.add(is);
            } else if (obj instanceof java.util.Map<?, ?> map) {
                try {
                    items.add(ItemStack.deserialize((java.util.Map<String, Object>) map));
                } catch (Exception e) {
                    if (plugin != null) {
                        plugin.getLogger().warning("Failed to deserialize Secret Santa gift item for recipient " + recipient + ": " + e.getMessage());
                    }
                }
            }
        }
        return items.toArray(new ItemStack[0]);
    }

    public ItemStack[] claimGift(UUID recipient) {
        ItemStack[] gift = getGift(recipient);
        if (gift == null || gift.length == 0) {
            return null;
        }
        config.set("gifts." + recipient.toString(), null);
        saveConfig();
        return gift;
    }

    public boolean hasGiftDeposited(UUID target) {
        return config.contains("gifts." + target.toString());
    }
}

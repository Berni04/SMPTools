package com.smp.smptools.locks;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.InventoryHolder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {

    private final SMPTools plugin;
    private final Map<String, UUID> containerOwners = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> containerTrusted = new ConcurrentHashMap<>();
    private File locksFile;
    private FileConfiguration locksConfig;

    public LockManager(SMPTools plugin) {
        this.plugin = plugin;
        loadLocks();
    }

    private void loadLocks() {
        if (plugin == null) return;
        locksFile = new File(plugin.getDataFolder(), "locks.yml");
        if (!locksFile.exists()) {
            try {
                locksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create locks.yml: " + e.getMessage());
            }
        }
        locksConfig = YamlConfiguration.loadConfiguration(locksFile);

        ConfigurationSection section = locksConfig.getConfigurationSection("locks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String ownerStr = section.getString(key + ".owner");
                if (ownerStr != null) {
                    try {
                        containerOwners.put(key, UUID.fromString(ownerStr));
                        List<String> trustedList = section.getStringList(key + ".trusted");
                        Set<UUID> trustedSet = ConcurrentHashMap.newKeySet();
                        for (String t : trustedList) {
                            try { trustedSet.add(UUID.fromString(t)); } catch (IllegalArgumentException ignored) {}
                        }
                        containerTrusted.put(key, trustedSet);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    public void saveLocks() {
        if (locksConfig == null) return;
        locksConfig.set("locks", null);

        for (Map.Entry<String, UUID> entry : containerOwners.entrySet()) {
            String key = entry.getKey();
            locksConfig.set("locks." + key + ".owner", entry.getValue().toString());
            Set<UUID> trusted = containerTrusted.get(key);
            if (trusted != null && !trusted.isEmpty()) {
                List<String> list = new ArrayList<>();
                for (UUID u : trusted) list.add(u.toString());
                locksConfig.set("locks." + key + ".trusted", list);
            }
        }

        try {
            locksConfig.save(locksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locks.yml: " + e.getMessage());
        }
    }

    public String getBlockKey(Block block) {
        if (block == null) return null;
        Location loc = block.getLocation();

        // Handle double chest pairing
        if (block.getState() instanceof Chest chest) {
            if (chest.getInventory() instanceof DoubleChestInventory doubleChest) {
                Location leftLoc = getLocationFromHolder(doubleChest.getLeftSide() != null ? doubleChest.getLeftSide().getHolder() : null);
                Location rightLoc = getLocationFromHolder(doubleChest.getRightSide() != null ? doubleChest.getRightSide().getHolder() : null);
                if (leftLoc != null && rightLoc != null) {
                    // Pick lower coordinate as canonical primary key for both halves
                    if (leftLoc.getBlockX() < rightLoc.getBlockX()) {
                        loc = leftLoc;
                    } else if (leftLoc.getBlockX() > rightLoc.getBlockX()) {
                        loc = rightLoc;
                    } else if (leftLoc.getBlockZ() < rightLoc.getBlockZ()) {
                        loc = leftLoc;
                    } else if (leftLoc.getBlockZ() > rightLoc.getBlockZ()) {
                        loc = rightLoc;
                    } else if (leftLoc.getBlockY() < rightLoc.getBlockY()) {
                        loc = leftLoc;
                    } else {
                        loc = rightLoc;
                    }
                }
            }
        }

        return getLocationKey(loc);
    }

    public String getLocationKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String worldIdentifier = loc.getWorld().getUID() != null
                ? loc.getWorld().getUID().toString()
                : loc.getWorld().getName();
        return worldIdentifier + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public Block getSurvivingDoubleChestHalf(Block brokenBlock) {
        if (brokenBlock == null) return null;
        if (brokenBlock.getState() instanceof Chest chest) {
            if (chest.getInventory() instanceof DoubleChestInventory doubleChest) {
                Location leftLoc = getLocationFromHolder(doubleChest.getLeftSide() != null ? doubleChest.getLeftSide().getHolder() : null);
                Location rightLoc = getLocationFromHolder(doubleChest.getRightSide() != null ? doubleChest.getRightSide().getHolder() : null);
                if (leftLoc != null && rightLoc != null) {
                    if (isSameBlockLocation(leftLoc, brokenBlock.getLocation())) {
                        return rightLoc.getBlock();
                    } else if (isSameBlockLocation(rightLoc, brokenBlock.getLocation())) {
                        return leftLoc.getBlock();
                    }
                }
            }
        }
        return null;
    }

    private boolean isSameBlockLocation(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ()
                && Objects.equals(loc1.getWorld(), loc2.getWorld());
    }

    public void removeOrMigrateLock(Block brokenBlock, Block survivingBlock) {
        String oldKey = getBlockKey(brokenBlock);
        if (oldKey == null || !containerOwners.containsKey(oldKey)) return;

        UUID owner = containerOwners.remove(oldKey);
        Set<UUID> trusted = containerTrusted.remove(oldKey);

        if (survivingBlock != null) {
            String newKey = getLocationKey(survivingBlock.getLocation());
            if (newKey != null && owner != null) {
                containerOwners.put(newKey, owner);
                if (trusted != null && !trusted.isEmpty()) {
                    containerTrusted.put(newKey, trusted);
                }
            }
        }
        saveLocks();
    }

    private Location getLocationFromHolder(InventoryHolder holder) {
        if (holder == null) return null;
        if (holder instanceof BlockState state) {
            return state.getLocation();
        }
        if (holder instanceof DoubleChest doubleChest) {
            return doubleChest.getLocation();
        }
        try {
            return (Location) holder.getClass().getMethod("getLocation").invoke(holder);
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean isContainer(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        return type.name().contains("CHEST") ||
               type.name().contains("BARREL") ||
               type.name().contains("SHULKER_BOX") ||
               type.name().contains("FURNACE") ||
               type.name().contains("SMOKER") ||
               type.name().contains("HOPPER") ||
               type.name().contains("DROPPER") ||
               type.name().contains("DISPENSER");
    }

    public boolean isLocked(Block block) {
        String key = getBlockKey(block);
        return key != null && containerOwners.containsKey(key);
    }

    public boolean canAccess(Block block, Player player) {
        if (player == null || block == null) return false;
        if (player.hasPermission("smptools.locks.admin")) return true;

        String key = getBlockKey(block);
        if (key == null || !containerOwners.containsKey(key)) return true;

        UUID owner = containerOwners.get(key);
        if (player.getUniqueId().equals(owner)) return true;

        Set<UUID> trusted = containerTrusted.get(key);
        return trusted != null && trusted.contains(player.getUniqueId());
    }

    public boolean lockContainer(Block block, Player owner) {
        if (!isContainer(block)) return false;
        String key = getBlockKey(block);
        if (key == null) return false;

        containerOwners.put(key, owner.getUniqueId());
        saveLocks();
        return true;
    }

    public boolean unlockContainer(Block block, Player player) {
        String key = getBlockKey(block);
        if (key == null || !containerOwners.containsKey(key)) return false;

        UUID owner = containerOwners.get(key);
        if (!player.getUniqueId().equals(owner) && !player.hasPermission("smptools.locks.admin")) {
            return false;
        }

        return removeLock(block);
    }

    public boolean removeLock(Block block) {
        String key = getBlockKey(block);
        if (key == null || !containerOwners.containsKey(key)) return false;

        containerOwners.remove(key);
        containerTrusted.remove(key);
        saveLocks();
        return true;
    }

    public boolean trustPlayer(Block block, Player owner, OfflinePlayer target) {
        String key = getBlockKey(block);
        if (key == null || !containerOwners.containsKey(key)) return false;
        if (!containerOwners.get(key).equals(owner.getUniqueId()) && !owner.hasPermission("smptools.locks.admin")) {
            return false;
        }

        containerTrusted.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(target.getUniqueId());
        saveLocks();
        return true;
    }

    public boolean untrustPlayer(Block block, Player owner, OfflinePlayer target) {
        String key = getBlockKey(block);
        if (key == null || !containerOwners.containsKey(key)) return false;
        if (!containerOwners.get(key).equals(owner.getUniqueId()) && !owner.hasPermission("smptools.locks.admin")) {
            return false;
        }

        Set<UUID> set = containerTrusted.get(key);
        if (set != null) {
            set.remove(target.getUniqueId());
            saveLocks();
            return true;
        }
        return false;
    }

    public String getOwnerName(Block block) {
        String key = getBlockKey(block);
        if (key == null || !containerOwners.containsKey(key)) return "Unknown";
        UUID uuid = containerOwners.get(key);
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() != null ? p.getName() : "Unknown";
    }

    public Map<String, UUID> getContainerOwners() {
        return containerOwners;
    }
}

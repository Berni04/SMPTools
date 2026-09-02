package com.smp.smptools.bounty;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BountyManager {

    private final SMPTools plugin;
    private final List<Bounty> bounties = new ArrayList<>();
    private final Map<UUID, List<ItemStack>> pendingRefunds = new ConcurrentHashMap<>();
    private File bountiesFile;
    private FileConfiguration bountiesConfig;
    private final Object fileLock = new Object();
    private final java.util.concurrent.ExecutorService saveExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "BountyManager-SaveThread");
        thread.setDaemon(true);
        return thread;
    });

    public BountyManager(SMPTools plugin) {
        this.plugin = plugin;
        loadBounties();
        startPeriodicCheck();
    }

    private void startPeriodicCheck() {
        if (plugin != null && plugin.isEnabled()) {
            try {
                Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    if (plugin.getConfig() != null && !plugin.getConfig().getBoolean("features.bounties.enabled", true)) {
                        return;
                    }
                    checkExpiredBounties();
                }, 1200L, 1200L);
            } catch (Exception ignored) {
                // In testing environment Bukkit scheduler might not be initialized
            }
        }
    }

    public synchronized void loadBounties() {
        if (plugin == null) return;
        bounties.clear();
        pendingRefunds.clear();

        bountiesFile = new File(plugin.getDataFolder(), "bounties.yml");
        if (!bountiesFile.exists()) {
            try {
                File parent = bountiesFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                bountiesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create bounties.yml: " + e.getMessage());
            }
        }
        bountiesConfig = YamlConfiguration.loadConfiguration(bountiesFile);

        ConfigurationSection section = bountiesConfig.getConfigurationSection("bounties");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                try {
                    String placerStr = section.getString(id + ".placerUuid");
                    String placerName = section.getString(id + ".placerName", "Unknown");
                    String targetStr = section.getString(id + ".targetUuid");
                    String targetName = section.getString(id + ".targetName", "Unknown");
                    long placedTs = section.getLong(id + ".placedTimestamp", System.currentTimeMillis());
                    
                    String killerStr = section.getString(id + ".killerUuid");
                    long killedTs = section.getLong(id + ".killedTimestamp", 0L);
                    boolean claimed = section.getBoolean(id + ".claimed", false);

                    List<?> itemsRaw = section.getList(id + ".items");
                    List<ItemStack> items = new ArrayList<>();
                    if (itemsRaw != null) {
                        for (Object obj : itemsRaw) {
                            if (obj instanceof ItemStack is) {
                                items.add(is);
                            }
                        }
                    }

                    UUID placerUuid = placerStr != null ? UUID.fromString(placerStr) : null;
                    UUID targetUuid = targetStr != null ? UUID.fromString(targetStr) : null;
                    UUID killerUuid = killerStr != null ? UUID.fromString(killerStr) : null;

                    if (placerUuid != null && targetUuid != null) {
                        Bounty bounty = new Bounty(id, placerUuid, placerName, targetUuid, targetName, items, placedTs, killerUuid, killedTs, claimed);
                        bounties.add(bounty);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load bounty " + id + ": " + e.getMessage());
                }
            }
        }

        ConfigurationSection refundSec = bountiesConfig.getConfigurationSection("pendingRefunds");
        if (refundSec != null) {
            for (String uuidStr : refundSec.getKeys(false)) {
                try {
                    UUID placerUuid = UUID.fromString(uuidStr);
                    List<?> itemsRaw = refundSec.getList(uuidStr);
                    List<ItemStack> items = new ArrayList<>();
                    if (itemsRaw != null) {
                        for (Object obj : itemsRaw) {
                            if (obj instanceof ItemStack is) {
                                items.add(is);
                            }
                        }
                    }
                    if (!items.isEmpty()) {
                        pendingRefunds.put(placerUuid, items);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load pending refunds for " + uuidStr + ": " + e.getMessage());
                }
            }
        }
    }

    private List<Bounty> getBountiesSnapshot() {
        synchronized (this) {
            List<Bounty> snapshot = new ArrayList<>(bounties.size());
            for (Bounty b : bounties) {
                List<ItemStack> clonedItems = new ArrayList<>();
                if (b.getItems() != null) {
                    for (ItemStack item : b.getItems()) {
                        if (item != null) {
                            clonedItems.add(item.clone());
                        }
                    }
                }
                snapshot.add(new Bounty(
                        b.getId(),
                        b.getPlacerUuid(),
                        b.getPlacerName(),
                        b.getTargetUuid(),
                        b.getTargetName(),
                        clonedItems,
                        b.getPlacedTimestamp(),
                        b.getKillerUuid(),
                        b.getKilledTimestamp(),
                        b.isClaimed()
                ));
            }
            return snapshot;
        }
    }

    private Map<UUID, List<ItemStack>> getPendingRefundsSnapshot() {
        synchronized (this) {
            Map<UUID, List<ItemStack>> map = new HashMap<>();
            for (Map.Entry<UUID, List<ItemStack>> entry : pendingRefunds.entrySet()) {
                List<ItemStack> itemCopies = new ArrayList<>();
                for (ItemStack is : entry.getValue()) {
                    if (is != null) {
                        itemCopies.add(is.clone());
                    }
                }
                map.put(entry.getKey(), itemCopies);
            }
            return map;
        }
    }

    private boolean writeBountiesToFile(List<Bounty> snapshot, Map<UUID, List<ItemStack>> refundsSnapshot) {
        if (bountiesFile == null) return true;
        synchronized (fileLock) {
            YamlConfiguration config = new YamlConfiguration();

            for (Bounty bounty : snapshot) {
                if (bounty.isClaimed()) {
                    continue; // Compact / omit claimed records once pending delivery is persisted
                }
                String path = "bounties." + bounty.getId();
                config.set(path + ".placerUuid", bounty.getPlacerUuid().toString());
                config.set(path + ".placerName", bounty.getPlacerName());
                config.set(path + ".targetUuid", bounty.getTargetUuid().toString());
                config.set(path + ".targetName", bounty.getTargetName());
                config.set(path + ".placedTimestamp", bounty.getPlacedTimestamp());
                if (bounty.getKillerUuid() != null) {
                    config.set(path + ".killerUuid", bounty.getKillerUuid().toString());
                }
                config.set(path + ".killedTimestamp", bounty.getKilledTimestamp());
                config.set(path + ".claimed", bounty.isClaimed());
                config.set(path + ".items", bounty.getItems());
            }

            for (Map.Entry<UUID, List<ItemStack>> entry : refundsSnapshot.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    config.set("pendingRefunds." + entry.getKey().toString(), entry.getValue());
                }
            }

            try {
                com.smp.smptools.utils.AtomicFileWriter.save(config, bountiesFile);
                return true;
            } catch (IOException e) {
                if (plugin != null) {
                    plugin.getLogger().severe("Could not save bounties.yml: " + e.getMessage());
                }
                return false;
            }
        }
    }

    public void saveBounties() {
        List<Bounty> snapshot = getBountiesSnapshot();
        Map<UUID, List<ItemStack>> refundsSnapshot = getPendingRefundsSnapshot();
        if (saveExecutor.isShutdown()) {
            writeBountiesToFile(snapshot, refundsSnapshot);
            return;
        }
        saveExecutor.submit(() -> writeBountiesToFile(snapshot, refundsSnapshot));
    }

    public boolean saveBountiesSync() {
        List<Bounty> snapshot = getBountiesSnapshot();
        Map<UUID, List<ItemStack>> refundsSnapshot = getPendingRefundsSnapshot();
        if (saveExecutor.isShutdown()) {
            return writeBountiesToFile(snapshot, refundsSnapshot);
        }
        try {
            return saveExecutor.submit(() -> writeBountiesToFile(snapshot, refundsSnapshot)).get();
        } catch (Exception e) {
            return writeBountiesToFile(snapshot, refundsSnapshot);
        }
    }

    public void shutdown() {
        saveBountiesSync();
        saveExecutor.shutdown();
    }

    public synchronized boolean createBounty(Player placer, Player target, List<ItemStack> items) {
        if (placer == null || target == null || items == null || items.isEmpty()) return false;

        Bounty bounty = new Bounty(
                UUID.randomUUID().toString(),
                placer.getUniqueId(),
                placer.getName(),
                target.getUniqueId(),
                target.getName(),
                new ArrayList<>(items),
                System.currentTimeMillis(),
                null,
                0L,
                false
        );

        bounties.add(bounty);
        saveBounties();
        return true;
    }

    public synchronized List<Bounty> getActiveBounties() {
        List<Bounty> list = new ArrayList<>();
        for (Bounty b : bounties) {
            if (b.isActive()) {
                list.add(b);
            }
        }
        return list;
    }

    public synchronized Map<UUID, List<Bounty>> getActiveBountiesGroupedByTarget() {
        Map<UUID, List<Bounty>> map = new LinkedHashMap<>();
        for (Bounty b : bounties) {
            if (b.isActive()) {
                map.computeIfAbsent(b.getTargetUuid(), k -> new ArrayList<>()).add(b);
            }
        }
        return map;
    }

    public synchronized List<Bounty> getActiveBountiesForTarget(UUID targetUuid) {
        List<Bounty> list = new ArrayList<>();
        for (Bounty b : bounties) {
            if (b.isActive() && b.getTargetUuid().equals(targetUuid)) {
                list.add(b);
            }
        }
        return list;
    }

    public synchronized boolean onPlayerKilled(Player victim, Player killer) {
        List<Bounty> active = getActiveBountiesForTarget(victim.getUniqueId());
        if (active.isEmpty()) return false;

        long now = System.currentTimeMillis();
        for (Bounty b : active) {
            b.setKillerUuid(killer.getUniqueId());
            b.setKilledTimestamp(now);
        }
        saveBounties();
        return true;
    }

    public synchronized List<Bounty> getClaimableBountiesForPlayer(Player player) {
        List<Bounty> list = new ArrayList<>();
        UUID uuid = player.getUniqueId();
        for (Bounty b : bounties) {
            if (b.isClaimableByKiller(uuid) || b.isRefundableToPlacer(uuid)) {
                list.add(b);
            }
        }
        return list;
    }

    public synchronized boolean claimOrRefundBounty(Bounty bounty, Player claimer) {
        if (bounty == null || bounty.isClaimed()) return false;

        UUID claimerUuid = claimer.getUniqueId();
        boolean isKiller = bounty.isClaimableByKiller(claimerUuid);
        boolean isRefund = bounty.isRefundableToPlacer(claimerUuid);

        if (!isKiller && !isRefund) return false;

        // Atomic claim state persistence: mark claimed and add items to pending delivery recovery state BEFORE disk write
        bounty.setClaimed(true);

        List<ItemStack> items = bounty.getItems();
        if (items != null && !items.isEmpty()) {
            List<ItemStack> pending = pendingRefunds.computeIfAbsent(claimerUuid, k -> new ArrayList<>());
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    pending.add(item.clone());
                }
            }
        }

        bounties.remove(bounty);
        saveBounties();

        if (claimer != null && claimer.isOnline()) {
            processPendingRefunds(claimer);
        }

        return true;
    }

    public synchronized void checkExpiredBounties() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (Bounty bounty : bounties) {
            if (!bounty.isClaimed() && bounty.getKillerUuid() != null && bounty.isExpired()) {
                bounty.setClaimed(true);
                changed = true;
                UUID placerUuid = bounty.getPlacerUuid();
                List<ItemStack> items = bounty.getItems();
                if (items != null && !items.isEmpty()) {
                    List<ItemStack> placerPending = pendingRefunds.computeIfAbsent(placerUuid, k -> new ArrayList<>());
                    for (ItemStack item : items) {
                        if (item != null && item.getType() != Material.AIR) {
                            placerPending.add(item.clone());
                        }
                    }
                    if (Bukkit.getServer() != null) {
                        Player placer = Bukkit.getPlayer(placerUuid);
                        if (placer != null && placer.isOnline()) {
                            processPendingRefunds(placer);
                        }
                    }
                }
            }
        }
        if (changed) {
            bounties.removeIf(Bounty::isClaimed);
            saveBounties();
        }
    }

    public synchronized void processPendingRefunds(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        List<ItemStack> pending = pendingRefunds.get(uuid);
        if (pending == null || pending.isEmpty()) return;
        if (player.getInventory() == null) return;

        boolean stateChanged = false;
        Iterator<ItemStack> iterator = pending.iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                iterator.remove();
                stateChanged = true;
                continue;
            }

            int originalAmount = item.getAmount();
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());

            int undeliveredAmount = 0;
            for (ItemStack rem : overflow.values()) {
                if (rem != null && rem.getType() != Material.AIR) {
                    undeliveredAmount += rem.getAmount();
                }
            }

            int deliveredAmount = originalAmount - undeliveredAmount;
            if (deliveredAmount > 0) {
                stateChanged = true;
                if (undeliveredAmount <= 0) {
                    iterator.remove();
                } else {
                    item.setAmount(undeliveredAmount);
                }
            }
        }

        if (pending.isEmpty()) {
            pendingRefunds.remove(uuid);
            stateChanged = true;
        }

        if (stateChanged) {
            saveBounties();
        }
    }

    public synchronized void checkPlayerJoin(Player player) {
        checkExpiredBounties();
        if (player != null && player.isOnline()) {
            processPendingRefunds(player);
        }
    }

    public synchronized Map<UUID, List<ItemStack>> getPendingRefunds() {
        Map<UUID, List<ItemStack>> copy = new HashMap<>();
        for (Map.Entry<UUID, List<ItemStack>> e : pendingRefunds.entrySet()) {
            List<ItemStack> itemCopies = new ArrayList<>();
            if (e.getValue() != null) {
                for (ItemStack is : e.getValue()) {
                    if (is != null) {
                        itemCopies.add(is.clone());
                    }
                }
            }
            copy.put(e.getKey(), Collections.unmodifiableList(itemCopies));
        }
        return Collections.unmodifiableMap(copy);
    }

    public synchronized List<Bounty> getBounties() {
        return Collections.unmodifiableList(bounties);
    }
}

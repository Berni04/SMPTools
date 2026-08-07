package com.smp.smptools.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class MongoStorageProvider implements StorageProvider {

    private final SMPTools plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> statsCollection;
    private MongoCollection<Document> tagsCollection;
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SMPTools-Mongo-Writer");
        thread.setDaemon(true);
        return thread;
    });

    public MongoStorageProvider(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        FileConfiguration config = plugin.getConfig();
        String uri = config.getString("storage.mongodb.uri", "mongodb://localhost:27017");
        String dbName = config.getString("storage.mongodb.database", "smptools");
        String prefix = config.getString("storage.mongodb.collection-prefix", "smptools_");

        try {
            ConnectionString connString = new ConnectionString(uri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .applyToSocketSettings(b -> b.connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS))
                    .applyToClusterSettings(b -> b.serverSelectionTimeout(3, TimeUnit.SECONDS))
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = mongoClient.getDatabase(dbName);
            this.statsCollection = database.getCollection(prefix + "stats");
            this.tagsCollection = database.getCollection(prefix + "tags");

            migrateFromFlatFileIfEmpty();
            plugin.getLogger().info("Storage provider set to MONGODB.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MONGODB storage provider: " + e.getMessage());
            if (this.mongoClient != null) {
                try {
                    this.mongoClient.close();
                } catch (Exception ignored) {}
            }
            this.mongoClient = null;
            this.database = null;
            this.statsCollection = null;
            this.tagsCollection = null;
        }
    }

    private String encodeKey(String key) {
        if (key == null) return null;
        return key.replace(".", "$DOT$");
    }

    private String decodeKey(String key) {
        if (key == null) return null;
        if (key.contains("$DOT$")) {
            return key.replace("$DOT$", ".");
        }
        return key;
    }

    private void migrateFromFlatFileIfEmpty() {
        if (statsCollection == null || tagsCollection == null) return;
        try {
            boolean tagsEmpty = tagsCollection.countDocuments() == 0;
            boolean statsEmpty = statsCollection.countDocuments() == 0;

            if (tagsEmpty || statsEmpty) {
                FlatFileStorageProvider flatfile = new FlatFileStorageProvider(plugin);

                if (tagsEmpty) {
                    Map<String, String> titles = flatfile.getAllPlayerTitles();
                    if (!titles.isEmpty()) {
                        plugin.getLogger().info("Performing initial migration of titles from FLATFILE to MONGODB...");
                        for (Map.Entry<String, String> entry : titles.entrySet()) {
                            try {
                                UUID uuid = UUID.fromString(entry.getKey());
                                Document filter = new Document("uuid", uuid.toString());
                                Document update = new Document("$set", new Document("title", entry.getValue()));
                                tagsCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to migrate title for " + entry.getKey() + ": " + e.getMessage());
                            }
                        }
                        plugin.getLogger().info("Successfully migrated titles to MONGODB.");
                    }
                }

                if (statsEmpty) {
                    Map<UUID, Map<String, Object>> stats = flatfile.loadAllPlayerStats();
                    if (!stats.isEmpty()) {
                        plugin.getLogger().info("Performing initial migration of stats from FLATFILE to MONGODB...");
                        for (Map.Entry<UUID, Map<String, Object>> playerEntry : stats.entrySet()) {
                            UUID uuid = playerEntry.getKey();
                            for (Map.Entry<String, Object> statEntry : playerEntry.getValue().entrySet()) {
                                try {
                                    Document filter = new Document("uuid", uuid.toString());
                                    String fieldPath = "stats." + encodeKey(statEntry.getKey());
                                    Document update = new Document("$set", new Document(fieldPath, statEntry.getValue()));
                                    statsCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Failed to migrate stat " + statEntry.getKey() + " for " + uuid + ": " + e.getMessage());
                                }
                            }
                        }
                        plugin.getLogger().info("Successfully migrated stats to MONGODB.");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed flatfile data migration to MONGODB: " + e.getMessage());
        }
    }

    private void executeAsyncWrite(Runnable runnable) {
        if (statsCollection == null && tagsCollection == null) return;
        if (writeExecutor.isShutdown()) {
            try {
                runnable.run();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute fallback Mongo write: " + e.getMessage());
            }
            return;
        }
        try {
            writeExecutor.submit(runnable);
        } catch (RejectedExecutionException e) {
            try {
                runnable.run();
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to execute fallback Mongo write after rejection: " + ex.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        if (writeExecutor != null) {
            writeExecutor.shutdown();
            List<Runnable> pending = null;
            try {
                if (!writeExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Mongo write executor did not terminate within timeout; forcing shutdown.");
                    pending = writeExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                plugin.getLogger().warning("Interrupted while awaiting Mongo write executor termination: " + e.getMessage());
                pending = writeExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            if (pending != null && !pending.isEmpty()) {
                if (plugin != null) {
                    plugin.getLogger().info("Executing " + pending.size() + " pending Mongo write task(s) synchronously on shutdown thread.");
                }
                for (Runnable task : pending) {
                    try {
                        task.run();
                    } catch (Exception e) {
                        if (plugin != null) {
                            plugin.getLogger().severe("Failed to execute pending Mongo write task during shutdown: " + e.getMessage());
                        }
                    }
                }
            }
        }
        if (mongoClient != null) {
            try {
                mongoClient.close();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void saveStat(UUID uuid, String statKey, Object value) {
        executeAsyncWrite(() -> {
            if (statsCollection == null) return;
            try {
                Document filter = new Document("uuid", uuid.toString());
                String fieldPath = "stats." + encodeKey(statKey);
                Document update = new Document("$set", new Document(fieldPath, value));
                statsCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save mongo stat for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public Object getStat(UUID uuid, String statKey, Object defaultValue) {
        if (statsCollection == null) return defaultValue;
        try {
            Document filter = new Document("uuid", uuid.toString());
            Document doc = statsCollection.find(filter).first();
            if (doc != null && doc.containsKey("stats")) {
                Document statsDoc = (Document) doc.get("stats");
                Object val = statsDoc.get(encodeKey(statKey));
                if (val == null) {
                    val = statsDoc.get(statKey.replace(".", "_"));
                }
                return StorageProvider.parseCanonicalValue(val, defaultValue);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get mongo stat for " + uuid + ": " + e.getMessage());
        }
        return defaultValue;
    }

    @Override
    public long getLongStat(UUID uuid, String statKey, long defaultValue) {
        Object val = getStat(uuid, statKey, defaultValue);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        if (val != null) {
            try {
                return Long.parseLong(val.toString());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    @Override
    public Map<String, Object> getAllPlayerStats(UUID uuid) {
        Map<String, Object> map = new HashMap<>();
        if (statsCollection == null) return map;
        try {
            Document filter = new Document("uuid", uuid.toString());
            Document doc = statsCollection.find(filter).first();
            if (doc != null && doc.containsKey("stats")) {
                Document statsDoc = (Document) doc.get("stats");
                for (Map.Entry<String, Object> entry : statsDoc.entrySet()) {
                    map.put(decodeKey(entry.getKey()), entry.getValue());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get all mongo stats for " + uuid + ": " + e.getMessage());
        }
        return map;
    }

    @Override
    public Map<UUID, Map<String, Object>> loadAllPlayerStats() {
        Map<UUID, Map<String, Object>> result = new HashMap<>();
        if (statsCollection == null) return result;
        try {
            for (Document doc : statsCollection.find()) {
                String uuidStr = doc.getString("uuid");
                if (uuidStr != null && doc.containsKey("stats")) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Document statsDoc = (Document) doc.get("stats");
                        Map<String, Object> map = new HashMap<>();
                        for (Map.Entry<String, Object> entry : statsDoc.entrySet()) {
                            map.put(decodeKey(entry.getKey()), entry.getValue());
                        }
                        result.put(uuid, map);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load all mongo stats: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void clearPlayerStats(UUID uuid) {
        executeAsyncWrite(() -> {
            if (statsCollection == null) return;
            try {
                Document doc = statsCollection.find(Filters.eq("uuid", uuid.toString())).first();
                if (doc != null) {
                    Object trail = null;
                    if (doc.containsKey("stats") && doc.get("stats") instanceof Document statsDoc) {
                        if (statsDoc.containsKey("active_trail")) {
                            trail = statsDoc.get("active_trail");
                        }
                    }
                    if (trail == null && doc.containsKey("active_trail")) {
                        trail = doc.get("active_trail");
                    }

                    if (trail != null) {
                        Document update = new Document("$set", new Document("stats", new Document("active_trail", trail)))
                                .append("$unset", new Document("active_trail", ""));
                        statsCollection.updateOne(Filters.eq("uuid", uuid.toString()), update);
                    } else {
                        statsCollection.deleteOne(Filters.eq("uuid", uuid.toString()));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to clear mongo stats for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void savePlayerTitle(UUID uuid, String title) {
        executeAsyncWrite(() -> {
            if (tagsCollection == null) return;
            try {
                Document filter = new Document("uuid", uuid.toString());
                Document update = new Document("$set", new Document("title", title));
                tagsCollection.updateOne(filter, update, new UpdateOptions().upsert(true));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save mongo title for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public String getPlayerTitle(UUID uuid) {
        if (tagsCollection == null) return null;
        try {
            Document doc = tagsCollection.find(Filters.eq("uuid", uuid.toString())).first();
            return doc != null ? doc.getString("title") : null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get mongo title for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void removePlayerTitle(UUID uuid) {
        executeAsyncWrite(() -> {
            if (tagsCollection == null) return;
            try {
                tagsCollection.deleteOne(Filters.eq("uuid", uuid.toString()));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to remove mongo title for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public Map<String, String> getAllPlayerTitles() {
        Map<String, String> map = new HashMap<>();
        if (tagsCollection == null) return map;
        try {
            for (Document doc : tagsCollection.find()) {
                String uuid = doc.getString("uuid");
                String title = doc.getString("title");
                if (uuid != null && title != null) {
                    map.put(uuid, title);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch mongo player titles: " + e.getMessage());
        }
        return map;
    }

    @Override
    public Map<String, Long> getLeaderboardStats(String statPath) {
        Map<String, Long> leaderboard = new LinkedHashMap<>();
        if (statsCollection == null) return leaderboard;
        Map<String, Long> rawMap = new HashMap<>();
        String safeKey = encodeKey(statPath);

        try {
            for (Document doc : statsCollection.find()) {
                String uuidStr = doc.getString("uuid");
                if (uuidStr != null && doc.containsKey("stats")) {
                    Document statsDoc = (Document) doc.get("stats");
                    Object valObj = statsDoc.get(safeKey);
                    if (valObj == null) {
                        valObj = statsDoc.get(statPath.replace(".", "_"));
                    }
                    if (valObj != null) {
                        try {
                            Object parsed = StorageProvider.parseCanonicalValue(valObj, 0L);
                            long val = (parsed instanceof Number) ? ((Number) parsed).longValue() : 0L;
                            rawMap.put(uuidStr, val);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch mongo leaderboard for " + statPath + ": " + e.getMessage());
        }

        rawMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(e -> leaderboard.put(e.getKey(), e.getValue()));

        return leaderboard;
    }
}


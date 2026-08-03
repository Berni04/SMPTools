package com.smp.smptools.storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bson.Document;

import java.util.*;

public class MongoStorageProvider implements StorageProvider {

    private final SMPTools plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> statsCollection;
    private MongoCollection<Document> tagsCollection;

    public MongoStorageProvider(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() {
        FileConfiguration config = plugin.getConfig();
        String uri = config.getString("storage.mongodb.uri", "mongodb://localhost:27017");
        String dbName = config.getString("storage.mongodb.database", "smptools");
        String prefix = config.getString("storage.mongodb.collection-prefix", "smptools_");

        this.mongoClient = MongoClients.create(uri);
        this.database = mongoClient.getDatabase(dbName);
        this.statsCollection = database.getCollection(prefix + "stats");
        this.tagsCollection = database.getCollection(prefix + "tags");

        plugin.getLogger().info("Storage provider set to MONGODB.");
    }

    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public void saveStat(UUID uuid, String statKey, Object value) {
        Document filter = new Document("uuid", uuid.toString());
        Document existing = statsCollection.find(filter).first();
        if (existing == null) {
            existing = new Document("uuid", uuid.toString());
        }
        Document statsDoc = (Document) existing.getOrDefault("stats", new Document());
        statsDoc.put(statKey.replace(".", "_"), value != null ? value.toString() : null);
        existing.put("stats", statsDoc);

        statsCollection.replaceOne(filter, existing, new ReplaceOptions().upsert(true));
    }

    @Override
    public Object getStat(UUID uuid, String statKey, Object defaultValue) {
        Document filter = new Document("uuid", uuid.toString());
        Document doc = statsCollection.find(filter).first();
        if (doc != null && doc.containsKey("stats")) {
            Document statsDoc = (Document) doc.get("stats");
            Object val = statsDoc.get(statKey.replace(".", "_"));
            return val != null ? val : defaultValue;
        }
        return defaultValue;
    }

    @Override
    public long getLongStat(UUID uuid, String statKey, long defaultValue) {
        Object val = getStat(uuid, statKey, null);
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
        Document filter = new Document("uuid", uuid.toString());
        Document doc = statsCollection.find(filter).first();
        if (doc != null && doc.containsKey("stats")) {
            Document statsDoc = (Document) doc.get("stats");
            for (Map.Entry<String, Object> entry : statsDoc.entrySet()) {
                map.put(entry.getKey().replace("_", "."), entry.getValue());
            }
        }
        return map;
    }

    @Override
    public Map<UUID, Map<String, Object>> loadAllPlayerStats() {
        Map<UUID, Map<String, Object>> result = new HashMap<>();
        for (Document doc : statsCollection.find()) {
            String uuidStr = doc.getString("uuid");
            if (uuidStr != null && doc.containsKey("stats")) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    Document statsDoc = (Document) doc.get("stats");
                    Map<String, Object> map = new HashMap<>();
                    for (Map.Entry<String, Object> entry : statsDoc.entrySet()) {
                        map.put(entry.getKey().replace("_", "."), entry.getValue());
                    }
                    result.put(uuid, map);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return result;
    }

    @Override
    public void clearPlayerStats(UUID uuid) {
        statsCollection.deleteOne(Filters.eq("uuid", uuid.toString()));
    }

    @Override
    public void savePlayerTitle(UUID uuid, String title) {
        Document filter = new Document("uuid", uuid.toString());
        Document doc = new Document("uuid", uuid.toString()).append("title", title);
        tagsCollection.replaceOne(filter, doc, new ReplaceOptions().upsert(true));
    }

    @Override
    public String getPlayerTitle(UUID uuid) {
        Document doc = tagsCollection.find(Filters.eq("uuid", uuid.toString())).first();
        return doc != null ? doc.getString("title") : null;
    }

    @Override
    public void removePlayerTitle(UUID uuid) {
        tagsCollection.deleteOne(Filters.eq("uuid", uuid.toString()));
    }

    @Override
    public Map<String, String> getAllPlayerTitles() {
        Map<String, String> map = new HashMap<>();
        for (Document doc : tagsCollection.find()) {
            String uuid = doc.getString("uuid");
            String title = doc.getString("title");
            if (uuid != null && title != null) {
                map.put(uuid, title);
            }
        }
        return map;
    }

    @Override
    public Map<String, Long> getLeaderboardStats(String statPath) {
        Map<String, Long> leaderboard = new LinkedHashMap<>();
        Map<String, Long> rawMap = new HashMap<>();
        String safeKey = statPath.replace(".", "_");

        for (Document doc : statsCollection.find()) {
            String uuidStr = doc.getString("uuid");
            if (uuidStr != null && doc.containsKey("stats")) {
                Document statsDoc = (Document) doc.get("stats");
                Object valObj = statsDoc.get(safeKey);
                if (valObj != null) {
                    try {
                        long val = Long.parseLong(valObj.toString());
                        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                        String name = player.getName() != null ? player.getName() : "Unknown";
                        rawMap.put(name, val);
                    } catch (Exception ignored) {}
                }
            }
        }

        rawMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(e -> leaderboard.put(e.getKey(), e.getValue()));

        return leaderboard;
    }
}

package com.smp.smptools.storage;

import com.smp.smptools.SMPTools;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class JdbcStorageProvider implements StorageProvider {

    private final SMPTools plugin;
    private final StorageType type;
    private HikariDataSource dataSource;
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SMPTools-JDBC-Writer");
        thread.setDaemon(true);
        return thread;
    });

    public JdbcStorageProvider(SMPTools plugin, StorageType type) {
        this.plugin = plugin;
        this.type = type;
    }

    @Override
    public void init() {
        FileConfiguration config = plugin.getConfig();
        HikariConfig hikari = new HikariConfig();

        if (type == StorageType.SQLITE) {
            File dbFile = new File(plugin.getDataFolder(), config.getString("storage.sqlite.file", "smptools.db"));
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(1);
        } else {
            String section = type == StorageType.MYSQL ? "storage.mysql" : "storage.mariadb";
            String host = config.getString(section + ".host", "localhost");
            int port = config.getInt(section + ".port", 3306);
            String db = config.getString(section + ".database", "smptools");
            String user = config.getString(section + ".username", "root");
            String pass = config.getString(section + ".password", "");
            int poolSize = config.getInt(section + ".pool-size", 10);
            boolean useSsl = config.getBoolean(section + ".use-ssl", false);

            String driver = type == StorageType.MYSQL ? "com.mysql.cj.jdbc.Driver" : "org.mariadb.jdbc.Driver";
            String jdbcPrefix = type == StorageType.MYSQL ? "jdbc:mysql" : "jdbc:mariadb";

            hikari.setJdbcUrl(String.format("%s://%s:%d/%s?useSSL=%b", jdbcPrefix, host, port, db, useSsl));
            hikari.setDriverClassName(driver);
            hikari.setUsername(user);
            hikari.setPassword(pass);
            hikari.setMaximumPoolSize(poolSize);
        }

        hikari.setPoolName("SMPTools-Pool");
        hikari.setConnectionTimeout(3000);
        hikari.setValidationTimeout(2000);
        hikari.setInitializationFailTimeout(3000);

        try {
            this.dataSource = new HikariDataSource(hikari);
            createTables();
            migrateFromFlatFileIfEmpty();
            plugin.getLogger().info("Storage provider initialized: " + type.name());
        } catch (Throwable e) {
            plugin.getLogger().severe("Failed to initialize JDBC storage provider (" + type.name() + "): " + e.getMessage() + ". Operating in graceful fallback mode.");
            this.dataSource = null;
        }
    }

    private void executeAsyncWrite(Runnable runnable) {
        if (dataSource == null) return;
        if (writeExecutor.isShutdown()) {
            try {
                runnable.run();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to execute JDBC fallback write during shutdown: " + e.getMessage());
            }
            return;
        }
        try {
            writeExecutor.submit(runnable);
        } catch (RejectedExecutionException e) {
            try {
                runnable.run();
            } catch (Exception ex) {
                plugin.getLogger().severe("Failed to execute JDBC fallback write after rejection: " + ex.getMessage());
            }
        }
    }

    private void createTables() {
        if (dataSource == null) return;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            if (type == StorageType.SQLITE) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS smptools_player_stats (" +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "stat_key VARCHAR(128) NOT NULL, " +
                        "stat_value TEXT, " +
                        "PRIMARY KEY (uuid, stat_key));");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS smptools_player_tags (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "title VARCHAR(128));");
            } else {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS smptools_player_stats (" +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "stat_key VARCHAR(128) NOT NULL, " +
                        "stat_value TEXT, " +
                        "PRIMARY KEY (uuid, stat_key));");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS smptools_player_tags (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "title VARCHAR(128));");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
        }
    }

    private void migrateFromFlatFileIfEmpty() {
        if (dataSource == null) return;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean statsEmpty = true;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM smptools_player_stats")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    statsEmpty = false;
                }
            }

            boolean tagsEmpty = true;
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM smptools_player_tags")) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tagsEmpty = false;
                }
            }

            if (statsEmpty && tagsEmpty) {
                FlatFileStorageProvider flatfile = new FlatFileStorageProvider(plugin);
                Map<String, String> titles = flatfile.getAllPlayerTitles();
                Map<UUID, Map<String, Object>> stats = flatfile.loadAllPlayerStats();

                if (!titles.isEmpty() || !stats.isEmpty()) {
                    plugin.getLogger().info("Performing initial migration of titles, trails, and stats from FLATFILE to JDBC...");

                    String titleSql = type == StorageType.SQLITE
                            ? "INSERT OR REPLACE INTO smptools_player_tags (uuid, title) VALUES (?, ?)"
                            : "INSERT INTO smptools_player_tags (uuid, title) VALUES (?, ?) ON DUPLICATE KEY UPDATE title = VALUES(title)";
                    try (PreparedStatement ps = conn.prepareStatement(titleSql)) {
                        for (Map.Entry<String, String> entry : titles.entrySet()) {
                            ps.setString(1, entry.getKey());
                            ps.setString(2, entry.getValue());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }

                    String statSql = type == StorageType.SQLITE
                            ? "INSERT OR REPLACE INTO smptools_player_stats (uuid, stat_key, stat_value) VALUES (?, ?, ?)"
                            : "INSERT INTO smptools_player_stats (uuid, stat_key, stat_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE stat_value = VALUES(stat_value)";
                    try (PreparedStatement ps = conn.prepareStatement(statSql)) {
                        for (Map.Entry<UUID, Map<String, Object>> playerEntry : stats.entrySet()) {
                            String uuidStr = playerEntry.getKey().toString();
                            for (Map.Entry<String, Object> statEntry : playerEntry.getValue().entrySet()) {
                                ps.setString(1, uuidStr);
                                ps.setString(2, statEntry.getKey());
                                ps.setString(3, statEntry.getValue() != null ? statEntry.getValue().toString() : null);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }

                    plugin.getLogger().info("Successfully migrated FLATFILE data to JDBC storage.");
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed flatfile data migration to JDBC: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (writeExecutor != null) {
            writeExecutor.shutdown();
            try {
                if (!writeExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    plugin.getLogger().severe("JDBC write executor did not terminate within timeout; forcing shutdown.");
                    List<Runnable> pending = writeExecutor.shutdownNow();
                    for (Runnable task : pending) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            plugin.getLogger().severe("Failed to execute pending JDBC write during shutdown: " + e.getMessage());
                        }
                    }
                }
            } catch (InterruptedException e) {
                plugin.getLogger().severe("Interrupted while awaiting JDBC write executor termination: " + e.getMessage());
                List<Runnable> pending = writeExecutor.shutdownNow();
                for (Runnable task : pending) {
                    try {
                        task.run();
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Failed to execute pending JDBC write during interruption: " + ex.getMessage());
                    }
                }
                Thread.currentThread().interrupt();
            }
        }
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                plugin.getLogger().severe("Error closing JDBC dataSource: " + e.getMessage());
            }
        }
    }

    @Override
    public void saveStat(UUID uuid, String statKey, Object value) {
        executeAsyncWrite(() -> {
            if (dataSource == null) return;
            String sql;
            if (type == StorageType.SQLITE) {
                sql = "INSERT OR REPLACE INTO smptools_player_stats (uuid, stat_key, stat_value) VALUES (?, ?, ?)";
            } else {
                sql = "INSERT INTO smptools_player_stats (uuid, stat_key, stat_value) VALUES (?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE stat_value = VALUES(stat_value)";
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, statKey);
                ps.setString(3, value != null ? value.toString() : null);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save stat for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public Object getStat(UUID uuid, String statKey, Object defaultValue) {
        if (dataSource == null) return defaultValue;
        String sql = "SELECT stat_value FROM smptools_player_stats WHERE uuid = ? AND stat_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, statKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("stat_value");
                    return StorageProvider.parseCanonicalValue(val, defaultValue);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get stat for " + uuid + ": " + e.getMessage());
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
        if (dataSource == null) return map;
        String sql = "SELECT stat_key, stat_value FROM smptools_player_stats WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("stat_key"), rs.getString("stat_value"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch all stats for " + uuid + ": " + e.getMessage());
        }
        return map;
    }

    @Override
    public Map<UUID, Map<String, Object>> loadAllPlayerStats() {
        Map<UUID, Map<String, Object>> result = new HashMap<>();
        if (dataSource == null) return result;
        String sql = "SELECT uuid, stat_key, stat_value FROM smptools_player_stats";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    result.computeIfAbsent(uuid, k -> new HashMap<>())
                          .put(rs.getString("stat_key"), rs.getString("stat_value"));
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load all stats: " + e.getMessage());
        }
        return result;
    }

    @Override
    public void clearPlayerStats(UUID uuid) {
        executeAsyncWrite(() -> {
            if (dataSource == null) return;
            String sql = "DELETE FROM smptools_player_stats WHERE uuid = ? AND stat_key != 'active_trail'";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to clear stats for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public void savePlayerTitle(UUID uuid, String title) {
        executeAsyncWrite(() -> {
            if (dataSource == null) return;
            String sql;
            if (type == StorageType.SQLITE) {
                sql = "INSERT OR REPLACE INTO smptools_player_tags (uuid, title) VALUES (?, ?)";
            } else {
                sql = "INSERT INTO smptools_player_tags (uuid, title) VALUES (?, ?) " +
                      "ON DUPLICATE KEY UPDATE title = VALUES(title)";
            }

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, title);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to save title for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public String getPlayerTitle(UUID uuid) {
        if (dataSource == null) return null;
        String sql = "SELECT title FROM smptools_player_tags WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("title");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get title for " + uuid + ": " + e.getMessage());
        }
        return null;
    }

    @Override
    public void removePlayerTitle(UUID uuid) {
        executeAsyncWrite(() -> {
            if (dataSource == null) return;
            String sql = "DELETE FROM smptools_player_tags WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to remove title for " + uuid + ": " + e.getMessage());
            }
        });
    }

    @Override
    public Map<String, String> getAllPlayerTitles() {
        Map<String, String> map = new HashMap<>();
        if (dataSource == null) return map;
        String sql = "SELECT uuid, title FROM smptools_player_tags";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("uuid"), rs.getString("title"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch player titles: " + e.getMessage());
        }
        return map;
    }

    @Override
    public Map<String, Long> getLeaderboardStats(String statPath) {
        Map<String, Long> leaderboard = new LinkedHashMap<>();
        if (dataSource == null) return leaderboard;
        String sql = "SELECT uuid, stat_value FROM smptools_player_stats WHERE stat_key = ?";
        Map<String, Long> rawMap = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statPath);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        String uuidStr = rs.getString("uuid");
                        String valStr = rs.getString("stat_value");
                        Object parsed = StorageProvider.parseCanonicalValue(valStr, 0L);
                        long val = (parsed instanceof Number) ? ((Number) parsed).longValue() : 0L;
                        rawMap.put(uuidStr, val);
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch leaderboard for " + statPath + ": " + e.getMessage());
        }

        rawMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(e -> leaderboard.put(e.getKey(), e.getValue()));

        return leaderboard;
    }
}

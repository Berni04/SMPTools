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

public class JdbcStorageProvider implements StorageProvider {

    private final SMPTools plugin;
    private final StorageType type;
    private HikariDataSource dataSource;

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

            hikari.setJdbcUrl(String.format("%s://%s:%d/%s?useSSL=%b&autoReconnect=true", jdbcPrefix, host, port, db, useSsl));
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
            plugin.getLogger().info("Storage provider initialized: " + type.name());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize JDBC storage provider (" + type.name() + "): " + e.getMessage());
            this.dataSource = null;
        }
    }

    private void executeAsyncWrite(Runnable runnable) {
        if (Bukkit.getServer() != null && Bukkit.isPrimaryThread() && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        } else {
            runnable.run();
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

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
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
            String sql = "DELETE FROM smptools_player_stats WHERE uuid = ?";
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
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                        String name = player.getName() != null ? player.getName() : "Unknown";
                        long val = Long.parseLong(rs.getString("stat_value"));
                        rawMap.put(name, val);
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

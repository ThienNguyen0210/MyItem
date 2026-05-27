package org.ThienNguyen.Listener.Station;

import org.ThienNguyen.Main;
import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class StationDatabase {
    private Connection connection;
    private final Main plugin;

    public StationDatabase(Main plugin) {
        this.plugin = plugin;
        initDatabase();
    }

    private synchronized void initDatabase() {
        try {
            File folder = new File(plugin.getDataFolder(), "Station");
            if (!folder.exists()) folder.mkdirs();

            File dbFile = new File(folder, "StationData.sqlite");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA synchronous = NORMAL;");

                
                stmt.execute("CREATE TABLE IF NOT EXISTS station_master (" +
                        "code TEXT PRIMARY KEY," +
                        "data_json TEXT NOT NULL," +
                        "lore_json TEXT," +
                        "display_name TEXT," +
                        "custom_model_data INTEGER," +
                        "pdc_json TEXT," +
                        "version INTEGER DEFAULT 1" +
                        ");");

                
                String[] columns = {
                        "ALTER TABLE station_master ADD COLUMN lore_json TEXT;",
                        "ALTER TABLE station_master ADD COLUMN display_name TEXT;",
                        "ALTER TABLE station_master ADD COLUMN custom_model_data INTEGER;",
                        "ALTER TABLE station_master ADD COLUMN pdc_json TEXT;"
                };

                for (String sql : columns) {
                    try {
                        stmt.execute(sql);
                    } catch (SQLException ignored) {
                        
                    }
                }

                
                stmt.execute("CREATE TABLE IF NOT EXISTS player_sync (" +
                        "player_uuid TEXT," +
                        "code TEXT," +
                        "last_version INTEGER," +
                        "PRIMARY KEY (player_uuid, code)" +
                        ");");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Lỗi khởi tạo SQLite Station!", e);
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initDatabase();
        }
        return connection;
    }

    public synchronized void deleteMasterData(String code) {
        String sql = "DELETE FROM station_master WHERE code = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.executeUpdate();
            plugin.getLogger().info("[StationDB] Đã xóa mã " + code + " khỏi station_master");
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi xóa station_master: " + e.getMessage());
        }
    }

    public int getMasterVersion(String code) {
        String sql = "SELECT version FROM station_master WHERE code = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("version");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi getMasterVersion: " + e.getMessage());
        }
        return 0;
    }

    public StationFullData getStationData(String code) {
        String sql = "SELECT data_json, lore_json, display_name, custom_model_data, pdc_json, version FROM station_master WHERE code = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Integer modelId = rs.getObject("custom_model_data") != null
                            ? rs.getInt("custom_model_data")
                            : null;

                    return new StationFullData(
                            rs.getString("data_json"),
                            rs.getString("lore_json"),
                            rs.getString("display_name"),
                            modelId,
                            rs.getString("pdc_json"),
                            rs.getInt("version")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi getStationData cho mã " + code + ": " + e.getMessage());
        }
        return null;
    }

    public int getPlayerSyncVersion(UUID uuid, String code) {
        String sql = "SELECT last_version FROM player_sync WHERE player_uuid = ? AND code = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, code);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("last_version");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi getPlayerSyncVersion: " + e.getMessage());
        }
        return 0;
    }

    public synchronized void updatePlayerSync(UUID uuid, String code, int version) {
        String sql = "INSERT OR REPLACE INTO player_sync (player_uuid, code, last_version) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, code);
            pstmt.setInt(3, version);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Lỗi updatePlayerSync: " + e.getMessage());
        }
    }

    public synchronized void updateMasterData(String code, String dataJson, String loreJson, String displayName, Integer customModelData, String pdcJson) {
        String sql = "INSERT INTO station_master (code, data_json, lore_json, display_name, custom_model_data, pdc_json, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, 1) " +
                "ON CONFLICT(code) DO UPDATE SET " +
                "data_json = ?, " +
                "lore_json = ?, " +
                "display_name = ?, " +
                "custom_model_data = ?, " +
                "pdc_json = ?, " +
                "version = version + 1";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            pstmt.setString(2, dataJson);
            pstmt.setString(3, loreJson);
            pstmt.setString(4, displayName);
            pstmt.setObject(5, customModelData); 
            pstmt.setString(6, pdcJson);

            
            pstmt.setString(7, dataJson);
            pstmt.setString(8, loreJson);
            pstmt.setString(9, displayName);
            pstmt.setObject(10, customModelData);
            pstmt.setString(11, pdcJson);

            pstmt.executeUpdate();
            plugin.getLogger().info("[StationDB] Đã cập nhật mã: " + code + " (Version hiện tại: " + getMasterVersion(code) + ")");
        } catch (SQLException e) {
            plugin.getLogger().severe("[StationDB] Lỗi khi updateMasterData cho mã " + code + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
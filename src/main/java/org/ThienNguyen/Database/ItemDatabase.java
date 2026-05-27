package org.ThienNguyen.Database;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDatabase {
    private Connection connection;

    public ItemDatabase(String path) {
        try {
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            createTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS saved_items (id TEXT PRIMARY KEY, data TEXT);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveItem(String id, ItemStack item) {
        String encodedItem = itemStackToBase64(item);
        String sql = "INSERT OR REPLACE INTO saved_items(id, data) VALUES(?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, encodedItem);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ItemStack loadItem(String id) {
        String sql = "SELECT data FROM saved_items WHERE id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return itemStackFromBase64(rs.getString("data"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Phương thức xóa vật phẩm (Khắc phục lỗi cannot find symbol deleteItem)
     */
    public void deleteItem(String id) {
        String sql = "DELETE FROM saved_items WHERE id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllIds() {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT id FROM saved_items;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getString("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    /**
     * Phương thức đóng kết nối (Khắc phục lỗi liên quan đến biến connection trong Main)
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
    private String itemStackToBase64(ItemStack item) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    
    private ItemStack itemStackFromBase64(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}
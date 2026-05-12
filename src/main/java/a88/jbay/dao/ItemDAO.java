package a88.jbay.dao;

import a88.jbay.common.item.Item;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemDAO {
    private final DatabaseController dbController;

    // Constructor for dependency injection
    public ItemDAO(DatabaseController dbController) {
        this.dbController = dbController;
    }

    public int insertItem(Item item) {
        String sql = "INSERT INTO items (name, type, `desc`, start_price, image) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = dbController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getType());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getInitPrice());
            stmt.setBytes(5, item.getImage());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return -1;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public Item findItemById(int itemId) {
        String sql = "SELECT id, name, type, `desc`, start_price, image FROM items WHERE id = ?";

        try (Connection connection = dbController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, itemId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new Item(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("desc"),
                    rs.getDouble("start_price"),
                    rs.getBytes("image")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

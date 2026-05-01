package a88.jbay.dao;

import a88.jbay.model.entity.item.Item;
import a88.jbay.testutil.TestDatabaseController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Test version of ItemDAO that uses H2 test database
 */
public class TestItemDAO {
    
    private static TestItemDAO instance;
    
    private TestItemDAO() {}
    
    public static synchronized TestItemDAO getInstance() {
        if (instance == null) {
            instance = new TestItemDAO();
        }
        return instance;
    }
    
    public int insertItem(Item item) {
        String sql = "INSERT INTO items (name, type, desc, start_price, image) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, item.getName());
            stmt.setString(2, item.getType());
            stmt.setString(3, item.getDescription());
            stmt.setDouble(4, item.getInitPrice());
            stmt.setBytes(5, item.getImage());

            int result = stmt.executeUpdate();
            
            if (result > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    public Item findItemById(int itemId) {
        String sql = "SELECT id, name, type, desc, start_price, image FROM items WHERE id = ?";
        try (Connection connection = TestDatabaseController.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Item(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("desc"),
                        rs.getDouble("start_price"),
                        rs.getBytes("image")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

package a88.jbay.dao;

import a88.jbay.common.item.Item;
import a88.jbay.dao.BaseDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.server.DatabaseController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemDAOImpl extends BaseDAO implements ItemDAO {

    public ItemDAOImpl(DatabaseController dbController) {
        super(dbController);
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        return new Item(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("desc"),
                rs.getDouble("start_price"),
                rs.getBytes("image")
        );
    }

    public int insertItem(Item item) {
        return executeInsert(
                "INSERT INTO items (name, type, `desc`, start_price, image) VALUES (?, ?, ?, ?, ?)",
                item.getName(), item.getType(), item.getDescription(), item.getInitPrice(), item.getImage()
        );
    }

    @Override
    public int insertItem(Connection connection, Item item) throws SQLException {
        return executeInsert(connection,
                "INSERT INTO items (name, type, `desc`, start_price, image) VALUES (?, ?, ?, ?, ?)",
                item.getName(), item.getType(), item.getDescription(), item.getInitPrice(), item.getImage()
        );
    }

    public Item findItemById(int itemId) {
        return executeQuery(
                "SELECT id, name, type, `desc`, start_price, image FROM items WHERE id = ?",
                rs -> rs.next() ? mapItem(rs) : null,
                itemId
        );
    }
}
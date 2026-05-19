package a88.jbay.dao;

import a88.jbay.common.item.Item;

import java.sql.Connection;
import java.sql.SQLException;

public interface ItemDAO {

    // --- transactional overload ---
    int insertItem(Connection connection, Item item) throws SQLException;

    // --- normal methods ---
    int insertItem(Item item);

    Item findItemById(int itemId);
}
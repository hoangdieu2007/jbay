package a88.jbay.dao;

import a88.jbay.common.item.Item;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ItemDAO {
    int insertItem(Item item);

    Item findItemById(int itemId);
}
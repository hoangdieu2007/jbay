package a88.jbay.dao;

import a88.jbay.common.item.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemDAOImplTest extends DaoTestBase {

    private final ItemDAO itemDAO = new ItemDAOImpl(dbController);

    @Test
    @DisplayName("Should insert item and return generated id")
    void testInsertItem() {
        Item item = new Item("Test Item", "ELECTRONICS", "A test item", 100.0, new byte[]{1, 2, 3});

        int id = itemDAO.insertItem(item);

        assertTrue(id > 0);
    }

    @Test
    @DisplayName("Should find item by id")
    void testFindItemById() throws Exception {
        int id = insertItem("My Item", "BOOK", "A book", 25.0, new byte[]{10, 20, 30});

        Item result = itemDAO.findItemById(id);

        assertNotNull(result);
        assertEquals("My Item", result.getName());
        assertEquals("BOOK", result.getType());
        assertEquals("A book", result.getDescription());
        assertEquals(25.0, result.getInitPrice(), 0.001);
        assertArrayEquals(new byte[]{10, 20, 30}, result.getImage());
    }

    @Test
    @DisplayName("Should return null when item not found")
    void testFindItemById_NotFound() {
        Item result = itemDAO.findItemById(999);

        assertNull(result);
    }

    @Test
    @DisplayName("Should insert item via transactional overload")
    void testInsertItem_Transactional() throws Exception {
        Item item = new Item("Tx Item", "FOOD", "Transactional insert", 15.0, new byte[]{});
        int id = itemDAO.insertItem(item);

        assertTrue(id > 0);
        Item saved = itemDAO.findItemById(id);
        assertNotNull(saved);
        assertEquals("Tx Item", saved.getName());
    }
}

package a88.jbay.data;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionCacheTest {

    private AuctionCache cache;
    private Auction auction1;
    private Auction auction2;

    @BeforeEach
    void setUp() {
        cache = new AuctionCache();
        Item item = new Item(1, "Test Item", "ELECTRONICS", "A test item", 100.0);
        UserData seller = new UserData(1, "seller1", "SELLER", "pass");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);

        auction1 = new Auction(1, item, seller, start, end);
        auction2 = new Auction(2, item, seller, start, end);
        auction2.setAuctionState(AuctionState.RUNNING);
    }

    @Test
    @DisplayName("Should store and retrieve auction")
    void testStoreAndGet() {
        cache.store(auction1);

        Auction result = cache.get(1);

        assertSame(auction1, result);
    }

    @Test
    @DisplayName("Should return null for non-existent auction")
    void testGet_NotFound() {
        Auction result = cache.get(999);

        assertNull(result);
    }

    @Test
    @DisplayName("Should check if cache contains auction")
    void testContains() {
        cache.store(auction1);

        assertTrue(cache.contains(1));
        assertFalse(cache.contains(999));
    }

    @Test
    @DisplayName("Should remove auction from cache")
    void testRemove() {
        cache.store(auction1);
        cache.remove(1);

        assertNull(cache.get(1));
        assertFalse(cache.contains(1));
    }

    @Test
    @DisplayName("Should get all auctions as collection")
    void testGetAll() {
        cache.store(auction1);
        cache.store(auction2);

        Collection<Auction> all = cache.getAll();

        assertEquals(2, all.size());
        assertTrue(all.contains(auction1));
        assertTrue(all.contains(auction2));
    }

    @Test
    @DisplayName("Should get all auctions as list")
    void testGetAllAsList() {
        cache.store(auction1);
        cache.store(auction2);

        List<Auction> list = cache.getAllAsList();

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Should get all auctions except those from a specific seller")
    void testGetAllExceptSeller() {
        UserData seller2 = new UserData(2, "seller2", "SELLER", "pass");
        Item item = new Item(1, "Item", "TYPE", "Desc", 50.0);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);

        Auction auction3 = new Auction(3, item, seller2, start, end);

        cache.store(auction1);
        cache.store(auction3);

        List<Auction> result = cache.getAllExceptSeller("seller1");

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getId());
    }

    @Test
    @DisplayName("Should return empty list when no auctions exist")
    void testGetAllExceptSeller_Empty() {
        List<Auction> result = cache.getAllExceptSeller("seller1");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should summarize all auctions as string")
    void testSummarize() {
        cache.store(auction1);

        String summary = cache.summarize();

        assertNotNull(summary);
        assertTrue(summary.contains("Test Item"));
        assertTrue(summary.contains("seller1"));
    }

    @Test
    @DisplayName("Should return empty string when no auctions")
    void testSummarize_Empty() {
        String summary = cache.summarize();

        assertEquals("", summary);
    }

    @Test
    @DisplayName("Should overwrite existing auction on store")
    void testStore_Overwrite() {
        cache.store(auction1);

        Item newItem = new Item(2, "Updated Item", "TYPE", "Desc", 200.0);
        UserData seller = new UserData(1, "seller1", "SELLER", "pass");
        Auction updated = new Auction(1, newItem, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        cache.store(updated);

        assertSame(updated, cache.get(1));
    }
}

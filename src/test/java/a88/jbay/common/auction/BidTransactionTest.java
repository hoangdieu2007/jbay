package a88.jbay.common.auction;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionTest {

    @Test
    void testConstructorAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        BidTransaction tx = new BidTransaction(1, "alice", 150.0, now);
        assertEquals(1, tx.getUserID());
        assertEquals("alice", tx.getUsername());
        assertEquals(150.0, tx.getAmt(), 0.001);
        assertEquals(now, tx.getTimestamp());
    }

    @Test
    void testToString() {
        BidTransaction tx = new BidTransaction(1, "alice", 150.0, LocalDateTime.of(2026, 5, 28, 13, 0));
        String result = tx.toString();
        assertTrue(result.contains("1"));
        assertTrue(result.contains("150"));
        assertTrue(result.contains("28/05/2026"));
    }

    // compareTo branches

    @Test
    void testCompareToLess() {
        BidTransaction lower = new BidTransaction(1, "a", 100.0, LocalDateTime.now());
        BidTransaction higher = new BidTransaction(2, "b", 200.0, LocalDateTime.now());
        assertTrue(lower.compareTo(higher) < 0);
    }

    @Test
    void testCompareToGreater() {
        BidTransaction lower = new BidTransaction(1, "a", 100.0, LocalDateTime.now());
        BidTransaction higher = new BidTransaction(2, "b", 200.0, LocalDateTime.now());
        assertTrue(higher.compareTo(lower) > 0);
    }

    @Test
    void testCompareToEqualAmountEarlierTimestampWins() {
        LocalDateTime earlier = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime later = LocalDateTime.of(2026, 1, 1, 12, 0);
        BidTransaction early = new BidTransaction(1, "a", 100.0, earlier);
        BidTransaction late = new BidTransaction(2, "b", 100.0, later);
        assertTrue(early.compareTo(late) > 0);
    }

    @Test
    void testCompareToEqualAmountLaterTimestamp() {
        LocalDateTime earlier = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime later = LocalDateTime.of(2026, 1, 1, 12, 0);
        BidTransaction early = new BidTransaction(1, "a", 100.0, earlier);
        BidTransaction late = new BidTransaction(2, "b", 100.0, later);
        assertTrue(late.compareTo(early) < 0);
    }

    @Test
    void testCompareToEqualAmountSameTimestamp() {
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 10, 0);
        BidTransaction tx1 = new BidTransaction(1, "a", 100.0, ts);
        BidTransaction tx2 = new BidTransaction(2, "b", 100.0, ts);
        assertTrue(tx1.compareTo(tx2) < 0);
    }
}

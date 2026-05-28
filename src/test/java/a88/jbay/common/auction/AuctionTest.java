package a88.jbay.common.auction;

import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Auction core state, subscription, and bid behavior.
 */
class AuctionTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 5, 15, 10, 0);

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = createAuction(BASE_TIME.plusHours(1), BASE_TIME.plusHours(2));
    }

    @Test
    @DisplayName("start should switch auction to running")
    void startSwitchesToRunning() {
        auction.start();

        assertEquals(AuctionState.RUNNING, auction.getAuctionState());
    }

    @Test
    @DisplayName("end should switch auction to finished")
    void endSwitchesToFinished() {
        auction.setAuctionState(AuctionState.RUNNING);

        auction.end();

        assertEquals(AuctionState.FINISHED, auction.getAuctionState());
    }

    @Test
    @DisplayName("cancel should switch auction to canceled")
    void cancelSwitchesToCanceled() {
        auction.cancel();

        assertEquals(AuctionState.CANCELED, auction.getAuctionState());
    }

    @Test
    @DisplayName("tick should start an opening auction after its start time")
    void tickStartsOpeningAuctionAfterStartTime() {
        auction = createAuction(BASE_TIME.minusMinutes(1), BASE_TIME.plusHours(1));

        boolean changed = auction.tick(BASE_TIME);

        assertTrue(changed);
        assertEquals(AuctionState.RUNNING, auction.getAuctionState());
    }

    @Test
    @DisplayName("tick should end a running auction after its end time")
    void tickEndsRunningAuctionAfterEndTime() {
        auction = createAuction(BASE_TIME.minusHours(2), BASE_TIME.minusMinutes(1));
        auction.setAuctionState(AuctionState.RUNNING);

        boolean changed = auction.tick(BASE_TIME);

        assertTrue(changed);
        assertEquals(AuctionState.FINISHED, auction.getAuctionState());
    }

    @Test
    @DisplayName("tick should leave auction unchanged before transition times")
    void tickDoesNotChangeAuctionBeforeTransitionTimes() {
        boolean changed = auction.tick(BASE_TIME);

        assertFalse(changed);
        assertEquals(AuctionState.OPENING, auction.getAuctionState());
    }

    @Test
    @DisplayName("subscribe should add unique user ids to subscribers")
    void subscribeAddsUniqueSubscribers() {
        auction.subscribe(1);
        auction.subscribe(2);
        auction.subscribe(1);

        assertTrue(auction.hasSubscribers());
        assertEquals(Set.of(1, 2), auction.getSubscribers());
    }

    @Test
    @DisplayName("unsubscribe should remove user ids from subscribers")
    void unsubscribeRemovesSubscribers() {
        auction.subscribe(1);
        auction.subscribe(2);

        auction.unsubscribe(1);

        assertEquals(Set.of(2), auction.getSubscribers());
        assertTrue(auction.hasSubscribers());

        auction.unsubscribe(2);

        assertTrue(auction.getSubscribers().isEmpty());
        assertFalse(auction.hasSubscribers());
    }

    @Test
    @DisplayName("subscribers view should be unmodifiable")
    void subscribersViewIsUnmodifiable() {
        auction.subscribe(1);

        Set<Integer> subscribers = auction.getSubscribers();

        assertThrows(UnsupportedOperationException.class, () -> subscribers.add(2));
        assertEquals(Set.of(1), auction.getSubscribers());
    }

    @Test
    @DisplayName("addBid should update current price, winner, winner id, and bid history")
    void addBidUpdatesPriceWinnerAndBidHistory() {
        auction.start();

        BidTransaction transaction = new BidTransaction(5, "bidder", 150.0, BASE_TIME);

        auction.addBid(150.0, transaction);

        assertEquals(150.0, auction.getCurrentPrice());
        assertEquals("bidder", auction.getWinner());
        assertEquals(5, auction.getWinnerId());
        assertEquals(1, auction.getBidHistory().size());
        assertSame(transaction, auction.getBidHistory().getFirst());
    }

    @Test
    @DisplayName("bid history view should be unmodifiable")
    void bidHistoryViewIsUnmodifiable() {
        BidTransaction transaction = new BidTransaction(5, "bidder", 150.0, BASE_TIME);
        auction.addBid(150.0, transaction);

        assertThrows(UnsupportedOperationException.class, () -> auction.getBidHistory().add(transaction));
    }

    @Test
    @DisplayName("updatePrice should reject bids below minimum increment")
    void updatePriceRejectsBidBelowMinimumIncrement() {
        auction.setAuctionState(AuctionState.RUNNING);
        auction.setMinIncrement(10.0);
        auction.addBid(100.0, new BidTransaction(5, "bidder", 100.0, BASE_TIME));

        BidTransaction lowBid = new BidTransaction(6, "other", 105.0, BASE_TIME.plusMinutes(1));

        assertThrows(Auction.BidRejected.class, () -> auction.placeBid(105.0, lowBid));
    }

    @Test
    @DisplayName("addBid should reject mismatched transaction amount")
    void addBidRejectsMismatchedTransactionAmount() {
        BidTransaction transaction = new BidTransaction(5, "bidder", 150.0, BASE_TIME);

        assertThrows(IllegalArgumentException.class, () -> auction.addBid(151.0, transaction));
    }

    @Test
    @DisplayName("min increment should be readable and writable")
    void minIncrementIsReadableAndWritable() {
        auction.setMinIncrement(5.0);

        assertEquals(5.0, auction.getMinIncrement());
    }

    @Test
    @DisplayName("setMinIncrement should reject negative values")
    void setMinIncrementRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> auction.setMinIncrement(-1.0));
    }

    private Auction createAuction(LocalDateTime startTime, LocalDateTime endTime) {
        Item item = new Item(1, "Test Item", "Generic", "A test item", 100.0);
        return new Auction(10, item, new UserData(1, "seller", "USER", "idk"), startTime, endTime);
    }
}

package a88.jbay.system;

import a88.jbay.common.auction.*;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.data.AuctionRepository;
import a88.jbay.data.BidRepository;
import a88.jbay.system.update.UpdateSystem;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BidSystemStressTest {

    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;
    private AuctionDAO auctionDAO;
    private BidDAO bidDAO;
    private UpdateSystem updateSystem;
    private BidSystem bidSystem;

    @BeforeEach
    void setUp() {
        auctionRepository = mock(AuctionRepository.class);
        bidRepository = mock(BidRepository.class);
        auctionDAO = mock(AuctionDAO.class);
        bidDAO = mock(BidDAO.class);
        updateSystem = mock(UpdateSystem.class);
        bidSystem = new BidSystem(auctionRepository, bidRepository, bidDAO, auctionDAO, updateSystem);
        bidSystem.setAutoBidDelayMs(0);
    }

    private Auction createRunningAuction(int id, double startPrice, double minIncrement) {
        Item item = new Item(1, "Stress Item", "ELECTRONICS", "Stress test item", startPrice);
        UserData seller = new UserData(1, "seller", "SELLER", "pass");
        Auction auction = new Auction(
                id, item, seller,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(1)
        );
        auction.setMinIncrement(minIncrement);
        auction.setAuctionState(AuctionState.RUNNING);
        return auction;
    }

    private void mockDependenciesForAuction(Auction auction) {
        when(auctionRepository.getActiveAuctionById(auction.getId())).thenReturn(auction);
        when(auctionRepository.getUsernameByUserId(anyInt()))
                .thenAnswer(inv -> "user" + inv.<Integer>getArgument(0));
        when(bidRepository.saveBid(anyInt(), any())).thenReturn(true);
        when(auctionDAO.updateEndTime(anyInt(), any())).thenReturn(true);
    }

    // ---------------------------------------------------------------
    // Test 1 – Same auction, 50 concurrent bidders
    // ---------------------------------------------------------------

    @Test
    @DisplayName("50 concurrent bids on same auction should serialize and produce consistent final state")
    void concurrentBidsOnSameAuction_shouldSerialize() throws Exception {
        int auctionId = 1;
        Auction auction = createRunningAuction(auctionId, 100.0, 0.0);
        mockDependenciesForAuction(auction);

        int numBidders = 50;
        StressResult result = runConcurrentBids(auctionId, numBidders, 100.0, 10.0);

        assertTrue(result.doneLatch.await(60, TimeUnit.SECONDS), "Not all bidders completed");
        assertTrue(result.errors.isEmpty(), "Exceptions during bidding: " + result.errors);

        assertNotNull(auction.getWinnerId(), "No winner after concurrent bids");
        assertNotNull(auction.getWinner(), "Winner name is null");
        assertTrue(auction.getCurrentPrice() >= 100.0,
                "Final price " + auction.getCurrentPrice() + " below start price");
        assertFalse(auction.getBidHistory().isEmpty(), "Bid history is empty");
        assertEquals(auction.getWinnerId(), auction.getBidHistory().getLast().getUserID(),
                "Winner ID does not match last bid in history");
        assertEquals(auction.getCurrentPrice(), auction.getBidHistory().getLast().getAmt(),
                "Current price does not match last bid amount");
    }

    // ---------------------------------------------------------------
    // Test 2 – 10 auctions × 5 bidders each, all concurrent
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Concurrent bids across 10 different auctions should all succeed without interference")
    void concurrentBidsOnDifferentAuctions_shouldBeParallel() throws Exception {
        int numAuctions = 10;
        int biddersPerAuction = 5;
        int totalThreads = numAuctions * biddersPerAuction;

        List<Auction> auctions = new ArrayList<>();
        for (int i = 0; i < numAuctions; i++) {
            Auction a = createRunningAuction(i + 1, 100.0, 0.0);
            mockDependenciesForAuction(a);
            auctions.add(a);
        }

        CyclicBarrier barrier = new CyclicBarrier(totalThreads);
        CountDownLatch done = new CountDownLatch(totalThreads);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int a = 0; a < numAuctions; a++) {
                int auctionId = a + 1;
                for (int b = 0; b < biddersPerAuction; b++) {
                    int userId = a * biddersPerAuction + b + 1;
                    double amount = 100.0 + b * 10.0;
                    executor.submit(() -> {
                        try {
                            barrier.await(10, TimeUnit.SECONDS);
                            bidSystem.placeBid(userId, auctionId, amount);
                        } catch (Throwable t) {
                            errors.add(t);
                        } finally {
                            done.countDown();
                        }
                    });
                }
            }
        }

        assertTrue(done.await(60, TimeUnit.SECONDS), "Not all threads completed");
        assertTrue(errors.isEmpty(), "Exceptions: " + errors);

        for (Auction auction : auctions) {
            assertNotNull(auction.getWinnerId(),
                    "Auction " + auction.getId() + " has no winner");
            assertFalse(auction.getBidHistory().isEmpty(),
                    "Auction " + auction.getId() + " bid history is empty");
            assertEquals(auction.getWinnerId(), auction.getBidHistory().getLast().getUserID(),
                    "Auction " + auction.getId() + " winner doesn't match last bid");
            assertEquals(auction.getCurrentPrice(), auction.getBidHistory().getLast().getAmt(),
                    "Auction " + auction.getId() + " price doesn't match last bid");
        }
    }

    // ---------------------------------------------------------------
    // Test 3 – 100 threads hammering the same auction
    // ---------------------------------------------------------------

    @Test
    @DisplayName("100 threads bidding concurrently on one auction should not corrupt internal state")
    void concurrentBids_shouldNotCorruptState() throws Exception {
        int auctionId = 1;
        Auction auction = createRunningAuction(auctionId, 100.0, 0.0);
        mockDependenciesForAuction(auction);

        int numBidders = 100;
        StressResult result = runConcurrentBids(auctionId, numBidders, 100.0, 5.0);

        assertTrue(result.doneLatch.await(60, TimeUnit.SECONDS), "Not all bidders completed");
        assertTrue(result.errors.isEmpty(), "Exceptions: " + result.errors);

        Integer winnerId = auction.getWinnerId();
        assertNotNull(winnerId, "No winner determined");

        List<BidTransaction> history = auction.getBidHistory();
        assertFalse(history.isEmpty(), "Bid history is empty");

        for (BidTransaction bid : history) {
            assertTrue(bid.getAmt() >= 100.0,
                    "Bid amount " + bid.getAmt() + " below start price");
        }

        BidTransaction lastBid = history.getLast();
        assertEquals(winnerId, lastBid.getUserID(),
                "Winner doesn't match last bid in history");
        assertEquals(auction.getCurrentPrice(), lastBid.getAmt(),
                "Current price doesn't match last bid amount");

        double prev = 0;
        for (BidTransaction bid : history) {
            assertTrue(bid.getAmt() > prev,
                    "Non-monotonic bid: " + bid.getAmt() + " after " + prev);
            prev = bid.getAmt();
        }
    }

    // ---------------------------------------------------------------
    // Extreme stress tests
    // ---------------------------------------------------------------

    @Test
    @DisplayName("500 concurrent bidders on one auction should not produce errors")
    void extreme_500ConcurrentBidsOnOneAuction() throws Exception {
        int auctionId = 1;
        Auction auction = createRunningAuction(auctionId, 100.0, 0.0);
        mockDependenciesForAuction(auction);

        int numBidders = 500;
        StressResult result = runConcurrentBids(auctionId, numBidders, 100.0, 2.0);

        assertTrue(result.doneLatch.await(60, TimeUnit.SECONDS), "Not all bidders completed");
        assertTrue(result.errors.isEmpty(), "Exceptions: " + result.errors);

        assertNotNull(auction.getWinnerId(), "No winner after 500 concurrent bids");
        assertFalse(auction.getBidHistory().isEmpty(), "Bid history is empty after 500 bids");

        BidTransaction lastBid = auction.getBidHistory().getLast();
        assertEquals(auction.getWinnerId(), lastBid.getUserID(),
                "Winner doesn't match last bid");
        assertEquals(auction.getCurrentPrice(), lastBid.getAmt(),
                "Price doesn't match last bid");
    }

    @Test
    @DisplayName("1000 concurrent bidders across 20 auctions should not produce errors")
    void extreme_1000BiddersAcross20Auctions() throws Exception {
        int numAuctions = 20;
        int biddersPerAuction = 50;
        int totalThreads = numAuctions * biddersPerAuction;

        List<Auction> auctions = new ArrayList<>();
        for (int i = 0; i < numAuctions; i++) {
            Auction a = createRunningAuction(i + 1, 100.0, 0.0);
            mockDependenciesForAuction(a);
            auctions.add(a);
        }

        CyclicBarrier barrier = new CyclicBarrier(totalThreads);
        CountDownLatch done = new CountDownLatch(totalThreads);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int a = 0; a < numAuctions; a++) {
                int auctionId = a + 1;
                for (int b = 0; b < biddersPerAuction; b++) {
                    int userId = a * biddersPerAuction + b + 1;
                    double amount = 100.0 + b * 2.0;
                    executor.submit(() -> {
                        try {
                            barrier.await(10, TimeUnit.SECONDS);
                            bidSystem.placeBid(userId, auctionId, amount);
                        } catch (Throwable t) {
                            errors.add(t);
                        } finally {
                            done.countDown();
                        }
                    });
                }
            }
        }

        assertTrue(done.await(60, TimeUnit.SECONDS), "Not all threads completed");
        assertTrue(errors.isEmpty(), "Exceptions: " + errors);

        for (Auction auction : auctions) {
            assertNotNull(auction.getWinnerId(),
                    "Auction " + auction.getId() + " has no winner");
            assertFalse(auction.getBidHistory().isEmpty(),
                    "Auction " + auction.getId() + " bid history is empty");
        }
    }

    @Test
    @DisplayName("2000 concurrent bid tasks on 40 auctions should not produce errors")
    void extreme_2000BidTasksAcross40Auctions() throws Exception {
        int numAuctions = 40;
        int biddersPerAuction = 50;
        int totalThreads = numAuctions * biddersPerAuction;

        List<Auction> auctions = new ArrayList<>();
        for (int i = 0; i < numAuctions; i++) {
            Auction a = createRunningAuction(i + 1, 100.0, 0.0);
            mockDependenciesForAuction(a);
            auctions.add(a);
        }

        CyclicBarrier barrier = new CyclicBarrier(totalThreads);
        CountDownLatch done = new CountDownLatch(totalThreads);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int a = 0; a < numAuctions; a++) {
                int auctionId = a + 1;
                for (int b = 0; b < biddersPerAuction; b++) {
                    int userId = a * biddersPerAuction + b + 1;
                    double amount = 100.0 + b * 2.0;
                    executor.submit(() -> {
                        try {
                            barrier.await(10, TimeUnit.SECONDS);
                            bidSystem.placeBid(userId, auctionId, amount);
                        } catch (Throwable t) {
                            errors.add(t);
                        } finally {
                            done.countDown();
                        }
                    });
                }
            }
        }

        assertTrue(done.await(60, TimeUnit.SECONDS), "Not all threads completed");
        assertTrue(errors.isEmpty(), "Exceptions: " + errors);
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private StressResult runConcurrentBids(int auctionId, int numBidders,
                                           double baseAmount, double step) {
        CyclicBarrier barrier = new CyclicBarrier(numBidders);
        CountDownLatch doneLatch = new CountDownLatch(numBidders);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < numBidders; i++) {
                int userId = i + 1;
                double amount = baseAmount + i * step;
                executor.submit(() -> {
                    try {
                        barrier.await(10, TimeUnit.SECONDS);
                        bidSystem.placeBid(userId, auctionId, amount);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        return new StressResult(doneLatch, errors);
    }

    private record StressResult(CountDownLatch doneLatch, List<Throwable> errors) {}
}

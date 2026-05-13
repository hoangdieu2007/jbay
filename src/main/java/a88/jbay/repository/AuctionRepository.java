package a88.jbay.repository;

import a88.jbay.common.auction.Auction;
import a88.jbay.common.auction.AuctionState;
import a88.jbay.common.auction.BidTransaction;
import a88.jbay.common.item.Item;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.AuctionDAO.AuctionData;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.util.JBayLogger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for managing active auctions in memory.
 * This class separates data storage concerns from business logic.
 */
public class AuctionRepository {

    private final Map<Integer, Auction> activeAuctions;

    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private final BidDAO bidDAO;

    private final JBayLogger logger;

    public AuctionRepository(
            AuctionDAO auctionDAO,
            ItemDAO itemDAO,
            UserDAO userDAO,
            BidDAO bidDAO
    ) {

        this.activeAuctions = new ConcurrentHashMap<>();

        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.bidDAO = bidDAO;

        this.logger = JBayLogger.getLogger(AuctionRepository.class);
    }

    /**
     * Store an active auction in memory.
     */
    public void storeActiveAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    /**
     * Get an active auction by ID.
     */
    public Auction getActiveAuction(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    /**
     * Remove an active auction from memory.
     */
    public void removeActiveAuction(int auctionId) {
        activeAuctions.remove(auctionId);
    }

    /**
     * Check if an auction is active.
     */
    public boolean isAuctionActive(int auctionId) {
        return activeAuctions.containsKey(auctionId);
    }

    /**
     * Get all active auctions.
     */
    public Collection<Auction> getAllActiveAuctions() {
        return activeAuctions.values();
    }

    public void loadActiveAuctions() {

        List<AuctionData> activeAuctionData =
                auctionDAO.findAllActiveAuctions();

        logger.info(
                "Loading " +
                        activeAuctionData.size() +
                        " active auctions from database"
        );

        for (AuctionData auctionData : activeAuctionData) {

            try {

                Auction auction = reconstructAuction(auctionData);

                if (auction != null) {

                    activeAuctions.put(
                            auction.getId(),
                            auction
                    );
                }

            } catch (Exception e) {

                logger.error(
                        "Failed to load auction " +
                                auctionData.id() +
                                ": " +
                                e.getMessage(),
                        e
                );
            }
        }
    }

    public List<Auction> getActiveAuctionList() {
        return new ArrayList<>(activeAuctions.values());
    }

    public List<Auction> getActiveAuctionListExceptForSeller(
            int userId
    ) {

        String sellerName =
                userDAO.findByUserId(userId).username();

        List<Auction> result = new ArrayList<>();

        for (Auction auction : activeAuctions.values()) {

            if (!auction.getSellerName().equals(sellerName)) {
                result.add(auction);
            }
        }

        return result;
    }

    public Auction getActiveAuctionById(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    public Auction getAuctionById(int auctionId) {

        Auction cachedAuction =
                activeAuctions.get(auctionId);

        if (cachedAuction != null) {
            return cachedAuction;
        }

        AuctionData auctionData =
                auctionDAO.findAuctionById(auctionId);

        if (auctionData == null) {
            return null;
        }

        Auction auction =
                reconstructAuction(auctionData);

        if (auction != null) {

            activeAuctions.put(
                    auction.getId(),
                    auction
            );
        }

        return auction;
    }

    public List<Auction> getAuctionsBySellerId(int sellerId) {

        List<AuctionData> auctionDataList =
                auctionDAO.findAuctionsBySellerId(sellerId);

        List<Auction> auctions = new ArrayList<>();

        for (AuctionData auctionData : auctionDataList) {

            Auction auction =
                    getAuctionById(auctionData.id());

            if (auction != null) {
                auctions.add(auction);
            }
        }

        return auctions;
    }

    public List<Auction> getAuctionsByWinnerId(int winnerId) {

        List<AuctionData> auctionDataList =
                auctionDAO.findAuctionsByWinnerId(winnerId);

        List<Auction> auctions = new ArrayList<>();

        for (AuctionData auctionData : auctionDataList) {

            Auction auction =
                    getAuctionById(auctionData.id());

            if (auction != null) {
                auctions.add(auction);
            }
        }

        return auctions;
    }

    public String listActiveAuctions() {

        StringBuilder result = new StringBuilder();

        for (Auction auction : activeAuctions.values()) {

            result.append(auction)
                    .append("\n\n");
        }

        return result.toString();
    }

    public boolean updateCurrentPrice(
            int auctionId,
            double newPrice,
            int winnerId
    ) {

        return auctionDAO.updateCurrentPrice(
                auctionId,
                newPrice,
                winnerId
        );
    }

    public String getUsernameByUserId(int userId) {
        return userDAO.findByUserId(userId).username();
    }

    public int insertItem(Item item) {
        return itemDAO.insertItem(item);
    }

    public int insertAuction(
            int itemId,
            int sellerId,
            double startPrice,
            double curPrice,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {

        return auctionDAO.insertAuction(
                itemId,
                sellerId,
                startPrice,
                curPrice,
                startTime,
                endTime
        );
    }

    public boolean updateEndTime(
            int auctionId,
            LocalDateTime newEndTime
    ) {

        return auctionDAO.updateEndTime(
                auctionId,
                newEndTime
        );
    }

    public boolean setAuctionState(
            int auctionId,
            AuctionState newState
    ) {

        return auctionDAO.setAuctionState(
                auctionId,
                newState
        );
    }

    /**
     * Dành riêng cho Admin: Lấy toàn bộ Auction từ DB và convert sang Object.
     */
    public List<Auction> getAllAuctionsForAdmin() {
        // Gọi DAO lấy danh sách dữ liệu thô (Record 10 biến)
        List<a88.jbay.dao.AuctionDAO.AuctionData> rawDataList = auctionDAO.getAllAuctionsForAdmin();
        List<Auction> adminAuctionList = new ArrayList<>();

        if (rawDataList != null) {
            for (a88.jbay.dao.AuctionDAO.AuctionData data : rawDataList) {
                // 1. Tạo Item: Gọi ĐÚNG tên hàm của Record là data.itemId(), data.itemName(), data.startPrice()
                a88.jbay.common.item.Item item = new a88.jbay.common.item.Item(
                        data.itemId(),
                        data.itemName(),
                        "UNKNOWN",
                        "",
                        data.startPrice()
                );

                // 2. Tạo Auction: Gọi ĐÚNG data.id(), data.sellerId()
                Auction auction = new Auction(
                        data.id(),
                        item,
                        String.valueOf(data.sellerId()),
                        data.startTime(),
                        data.endTime()
                );

                // 3. Fix lỗi State: Ép kiểu từ String (data.state()) sang Enum (AuctionState)
                try {
                    auction.setAuctionState(a88.jbay.common.auction.AuctionState.valueOf(data.state()));
                } catch (Exception e) {
                    auction.setAuctionState(a88.jbay.common.auction.AuctionState.OPENING);
                }

                // BỎ QUA việc gọi setCurrentPrice và setWinner vì Auction class của ông không có 2 hàm này
                // Và bảng UI Admin cũng không hiển thị chúng!

                adminAuctionList.add(auction);
            }
        }
        return adminAuctionList;
    }

    /**
     * Reconstruct auction from persistent data.
     * This method restores state only.
     * No side effects should happen here.
     */
    private Auction reconstructAuction(
            AuctionData auctionData
    ) {

        Item item =
                itemDAO.findItemById(
                        auctionData.itemId()
                );

        if (item == null) {

            logger.error(
                    "Item not found for auction " +
                            auctionData.id()
            );

            return null;
        }

        String sellerName =
                userDAO.findByUserId(
                        auctionData.sellerId()
                ).username();

        logger.debug(
                "Loading auction: " +
                        auctionData.id() +
                        " - " +
                        item.getName() +
                        " - State: " +
                        auctionData.state()
        );

        Auction auction = new Auction(
                auctionData.id(),
                item,
                sellerName,
                auctionData.startTime(),
                auctionData.endTime()
        );

        auction.setAuctionState(
                AuctionState.valueOf(
                        auctionData.state()
                )
        );

        List<BidDAO.BidData> bidHistory =
                bidDAO.findBidHistoryByAuctionId(
                        auctionData.id()
                );

        Set<Integer> bidders = new HashSet<>();

        for (BidDAO.BidData bidData : bidHistory) {

            String bidderName =
                    userDAO.findByUserId(
                            bidData.userId()
                    ).username();

            BidTransaction tx =
                    new BidTransaction(
                            bidData.userId(),
                            bidderName,
                            bidData.amount(),
                            bidData.time()
                    );

            /*
             * IMPORTANT:
             * Use reconstruction-safe method.
             * DO NOT call live business workflow methods here.
             */
            auction.addBid(
                    bidData.amount(),
                    tx
            );

            bidders.add(bidData.userId());
        }

        // subscribe all bidders

        for (Integer bidderId : bidders) {
            auction.subscribe(bidderId);
        }

        // always subscribe seller

        auction.subscribe(
                auctionData.sellerId()
        );

        return auction;
    }
}
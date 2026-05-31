package a88.jbay.data;

import a88.jbay.common.auction.*;
import a88.jbay.common.item.Item;
import a88.jbay.common.user.UserData;
import a88.jbay.dao.AuctionDAO;
import a88.jbay.dao.BidDAO;
import a88.jbay.dao.ItemDAO;
import a88.jbay.dao.UserDAO;
import a88.jbay.server.DatabaseController;
import a88.jbay.util.JBayLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AuctionRepository {

    private final AuctionCache cache;
    private final AuctionFactory factory;

    private final AuctionDAO auctionDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;
    private final BidDAO bidDAO;

    private final DatabaseController dbController;
    private final JBayLogger logger;

    public AuctionRepository(
            DatabaseController dbController,
            AuctionDAO auctionDAO,
            ItemDAO itemDAO,
            UserDAO userDAO,
            BidDAO bidDAO
    ) {
        this.dbController = dbController;
        this.auctionDAO = auctionDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
        this.bidDAO = bidDAO;

        this.cache = new AuctionCache();
        this.factory = new AuctionFactory(itemDAO, userDAO, bidDAO);
        this.logger = JBayLogger.getLogger(AuctionRepository.class);
    }

    // --- Cache delegation ---

    public void storeActiveAuction(Auction auction) {
        cache.store(auction);
    }

    public void removeActiveAuction(int auctionId) {
        cache.remove(auctionId);
    }

    public Auction getActiveAuctionById(int auctionId) {
        return cache.get(auctionId);
    }

    public boolean isAuctionActive(int auctionId) {
        return cache.contains(auctionId);
    }

    public Collection<Auction> getAllActiveAuctions() {
        return cache.getAll();
    }

    public List<Auction> getActiveAuctionList() {
        return cache.getAllAsList();
    }

    public List<Auction> getActiveAuctionListExceptForSeller(int userId) {
        UserData userData = userDAO.findByUserId(userId);
        String sellerName = userData == null ? null : userData.username();
        return cache.getAllExceptSeller(sellerName);
    }

    public String listActiveAuctions() {
        return cache.summarize();
    }

    // --- Lookups (cache-first) ---

    public Auction getAuctionById(int auctionId) {
        Auction cached = cache.get(auctionId);
        if (cached != null) return cached;

        AuctionData data = auctionDAO.findAuctionById(auctionId);
        if (data == null) return null;

        Auction auction = factory.reconstruct(data);
        if (auction != null && (auction.getAuctionState() == AuctionState.OPENING
                || auction.getAuctionState() == AuctionState.RUNNING)) {
            cache.store(auction);
        }
        return auction;
    }

    public List<Auction> getAuctionsBySellerId(int sellerId) {
        return auctionDAO.findAuctionsBySellerId(sellerId).stream()
                .map(data -> getAuctionById(data.id()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Auction> getAuctionsByWinnerId(int winnerId) {
        return auctionDAO.findAuctionsByWinnerId(winnerId).stream()
                .map(data -> getAuctionById(data.id()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Auction> getAllAuctionsForAdmin() {
        return auctionDAO.getAllAuctionsForAdmin().stream()
                .map(data -> {
                    // 1. Ưu tiên lấy hàng từ RAM (Cache) nếu phiên đấu giá đang RUNNING
                    // -> Việc này đảm bảo Admin luôn nhận được cục dữ liệu có Bids History real-time mới nhất
                    Auction cached = cache.get(data.id());
                    if (cached != null) return cached;

                    // 2. Nếu phiên đấu giá đã FINISHED hoặc CANCELED (không còn trong RAM),
                    // -> Dùng lệnh reconstruct() gốc để ép Server vào Database móc lên ĐẦY ĐỦ cả mảng byte Ảnh và lịch sử Bids
                    try {
                        return factory.reconstruct(data);
                    } catch (Exception e) {
                        logger.error("Lỗi khi tải full thông tin cho Admin - Auction ID: " + data.id(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String getUsernameByUserId(int userId) {
        UserData userData = userDAO.findByUserId(userId);
        return userData == null ? "Unknown" : userData.username();
    }

    // --- Persistence ---

    public boolean setAuctionState(int auctionId, AuctionState newState) {
        boolean updated = auctionDAO.setAuctionState(auctionId, newState);
        if (updated) {
            Auction cached = cache.get(auctionId);
            if (cached != null) cached.setAuctionState(newState);
        }
        return updated;
    }

    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        boolean updated = auctionDAO.updateEndTime(auctionId, newEndTime);
        if (updated) {
            Auction cached = cache.get(auctionId);
            if (cached != null) cached.setEndTime(newEndTime);
        }
        return updated;
    }

    // --- Transactional operations ---

    /**
    * @deprecated
    *
     **/
//    public int insertItemAndAuction(Item item, int sellerId,
//                                    LocalDateTime start, LocalDateTime end) {
//        return insertItemAndAuction(item, sellerId, 0.0, start, end);
//    }

    public int insertItemAndAuction(Item item, int sellerId, double minIncrement,
                                    LocalDateTime start, LocalDateTime end) {
        try (Connection connection = dbController.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int itemId = itemDAO.insertItem(connection, item);
                int auctionId = auctionDAO.insertAuction(connection,
                        itemId, sellerId, item.getInitPrice(), minIncrement, start, end);

                connection.commit();
                return auctionId;

            } catch (Exception e) {
                connection.rollback();
                logger.error("Transaction failed creating auction for item: " + item.getName());
                e.printStackTrace();
                return -1;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // --- Startup ---

    public void loadActiveAuctions() {
        List<AuctionData> activeAuctionData = auctionDAO.findAllActiveAuctions();
        logger.info("Loading " + activeAuctionData.size() + " active auctions from database");

        activeAuctionData.stream()
                .map(data -> {
                    try {
                        return factory.reconstruct(data);
                    } catch (Exception e) {
                        logger.error("Failed to load auction " + data.id() + ": " + e.getMessage(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(cache::store);
    }
}

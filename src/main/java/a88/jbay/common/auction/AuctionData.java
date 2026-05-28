package a88.jbay.common.auction;

import java.time.LocalDateTime;

public record AuctionData(
        int id,
        int itemId,
        int sellerId,
        double startPrice,
        double curPrice,
        double minIncrement,
        Integer winnerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String state,
        String itemName
) {}

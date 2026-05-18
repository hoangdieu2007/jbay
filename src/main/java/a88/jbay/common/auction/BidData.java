package a88.jbay.common.auction;

import java.time.LocalDateTime;

public record BidData(
        int userId,
        int auctionId,
        double amount,
        LocalDateTime time
) {}
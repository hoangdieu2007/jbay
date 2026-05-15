package a88.jbay.system;

import java.io.Serializable;

/**
 * Configuration for auto-bidding functionality
 */
public class AutoBidConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private int userId;
    private double maxAmount;
    private double increment;

    public AutoBidConfig(int userId, double maxAmount, double increment) {
        this.userId = userId;
        this.maxAmount = maxAmount;
        this.increment = increment;
    }

    public AutoBidConfig(double maxAmount, double increment) {
        this(0, maxAmount, increment);
    }

    public int getUserId() {
        return userId;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public double getIncrement() {
        return increment;
    }
}

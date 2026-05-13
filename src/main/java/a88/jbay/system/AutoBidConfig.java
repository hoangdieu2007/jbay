package a88.jbay.system;

import java.io.Serializable;

/**
 * Configuration for auto-bidding functionality
 */
public class AutoBidConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private double maxAmount;
    private double increment;

    public AutoBidConfig(double maxAmount, double increment) {
        this.maxAmount = maxAmount;
        this.increment = increment;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public double getIncrement() {
        return increment;
    }
}

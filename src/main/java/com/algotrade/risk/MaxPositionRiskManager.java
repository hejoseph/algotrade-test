package com.algotrade.risk;

import com.algotrade.model.Order;
import com.algotrade.model.Side;
import com.algotrade.pipeline.RiskManager;

public class MaxPositionRiskManager implements RiskManager {
    private final PositionManager positionManager;
    private final String symbol;
    private final long maxAbsolutePosition;

    public MaxPositionRiskManager(PositionManager positionManager, String symbol, long maxAbsolutePosition) {
        this.positionManager = positionManager;
        this.symbol = symbol;
        this.maxAbsolutePosition = maxAbsolutePosition;
    }

    @Override
    public boolean checkOrder(Order order) {
        if (!order.getSymbol().equals(symbol)) {
            return true; // Not managing this symbol, so approve
        }

        long currentPosition = positionManager.getPosition(symbol);
        long potentialNewPosition;

        if (order.getSide() == Side.BUY) {
            potentialNewPosition = currentPosition + order.getQuantity();
        } else { // SELL
            potentialNewPosition = currentPosition - order.getQuantity();
        }

        if (Math.abs(potentialNewPosition) > maxAbsolutePosition) {
            System.out.println("Risk check failed: Order " + order.getOrderId() + " would exceed max position for " + symbol);
            return false; // Order would exceed max position
        }
        return true; // Order approved by risk manager
    }
}

package com.algotrade.risk;

import com.algotrade.model.Order;
import com.algotrade.model.Side;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PositionManager {
    private final Map<String, Long> positions;

    public PositionManager() {
        this.positions = new ConcurrentHashMap<>();
    }

    public synchronized void updatePosition(Order order) {
        positions.compute(order.getSymbol(), (symbol, quantity) -> {
            if (quantity == null) {
                quantity = 0L;
            }
            if (order.getSide() == Side.BUY) {
                return quantity + order.getQuantity();
            } else {
                return quantity - order.getQuantity();
            }
        });
    }

    public long getPosition(String symbol) {
        return positions.getOrDefault(symbol, 0L);
    }

    public Map<String, Long> getAllPositions() {
        return positions;
    }
}

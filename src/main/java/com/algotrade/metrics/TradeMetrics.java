package com.algotrade.metrics;

import com.algotrade.model.Order;
import com.algotrade.model.Trade;
import com.algotrade.model.Side;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class TradeMetrics {
    private final Map<String, Double> pnl;
    private final Map<String, AtomicLong> totalOrderedQuantity;
    private final Map<String, AtomicLong> totalFilledQuantity;
    private final Map<String, AtomicReference<Double>> lastPrice;

    public TradeMetrics() {
        this.pnl = new ConcurrentHashMap<>();
        this.totalOrderedQuantity = new ConcurrentHashMap<>();
        this.totalFilledQuantity = new ConcurrentHashMap<>();
        this.lastPrice = new ConcurrentHashMap<>();
    }

    public synchronized void recordOrder(Order order) {
        totalOrderedQuantity.computeIfAbsent(order.getSymbol(), s -> new AtomicLong(0)).addAndGet(order.getQuantity());
    }

    public synchronized void recordTrade(Trade trade) {
        totalFilledQuantity.computeIfAbsent(trade.getSymbol(), s -> new AtomicLong(0)).addAndGet(trade.getQuantity());
        lastPrice.computeIfAbsent(trade.getSymbol(), s -> new AtomicReference<>(0.0)).set(trade.getPrice());

        // Simple PnL calculation (unrealized for simplicity here, realized PnL would be more complex)
        pnl.compute(trade.getSymbol(), (symbol, currentPnl) -> {
            if (currentPnl == null) {
                currentPnl = 0.0;
            }
            if (trade.getSide() == Side.BUY) {
                return currentPnl - (trade.getPrice() * trade.getQuantity());
            } else { // SELL
                return currentPnl + (trade.getPrice() * trade.getQuantity());
            }
        });
    }

    public double getPnl(String symbol) {
        return pnl.getOrDefault(symbol, 0.0);
    }

    public double getFillRatio(String symbol) {
        long ordered = totalOrderedQuantity.getOrDefault(symbol, new AtomicLong(0)).get();
        long filled = totalFilledQuantity.getOrDefault(symbol, new AtomicLong(0)).get();
        return ordered == 0 ? 0.0 : (double) filled / ordered;
    }

    public Map<String, Double> getAllPnL() {
        return pnl;
    }

    public Map<String, Double> getAllFillRatios() {
        Map<String, Double> fillRatios = new ConcurrentHashMap<>();
        totalOrderedQuantity.keySet().forEach(symbol -> fillRatios.put(symbol, getFillRatio(symbol)));
        return fillRatios;
    }

    public Double getLastPrice(String symbol) {
        return lastPrice.getOrDefault(symbol, new AtomicReference<>(0.0)).get();
    }
}

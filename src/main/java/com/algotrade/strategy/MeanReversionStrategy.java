package com.algotrade.strategy;

import com.algotrade.model.MarketData;
import com.algotrade.model.Order;
import com.algotrade.model.OrderType;
import com.algotrade.model.Side;
import com.algotrade.pipeline.StrategyProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MeanReversionStrategy implements StrategyProcessor {
    private final String symbol;
    private final int lookbackPeriod;
    private final double priceThreshold;
    private final long orderQuantity;
    private final Map<String, List<Double>> priceHistory;

    public MeanReversionStrategy(String symbol, int lookbackPeriod, double priceThreshold, long orderQuantity) {
        this.symbol = symbol;
        this.lookbackPeriod = lookbackPeriod;
        this.priceThreshold = priceThreshold;
        this.orderQuantity = orderQuantity;
        this.priceHistory = new ConcurrentHashMap<>();
        this.priceHistory.put(symbol, new ArrayList<>());
    }

    @Override
    public List<Order> processMarketData(MarketData marketData) {
        List<Order> orders = new ArrayList<>();

        if (!marketData.getSymbol().equals(symbol)) {
            return orders;
        }

        List<Double> history = priceHistory.get(symbol);
        history.add((marketData.getBidPrice() + marketData.getAskPrice()) / 2.0);

        if (history.size() > lookbackPeriod) {
            history.remove(0);
        }

        if (history.size() == lookbackPeriod) {
            double sum = history.stream().mapToDouble(Double::doubleValue).sum();
            double movingAverage = sum / lookbackPeriod;

            if (marketData.getAskPrice() < movingAverage * (1 - priceThreshold)) {
                // Price is significantly below moving average, consider buying
                orders.add(new Order(symbol, OrderType.LIMIT, Side.BUY, marketData.getAskPrice(), orderQuantity));
                System.out.println("MeanReversionStrategy: BUY order generated for " + symbol + " at " + marketData.getAskPrice());
            } else if (marketData.getBidPrice() > movingAverage * (1 + priceThreshold)) {
                // Price is significantly above moving average, consider selling
                orders.add(new Order(symbol, OrderType.LIMIT, Side.SELL, marketData.getBidPrice(), orderQuantity));
                System.out.println("MeanReversionStrategy: SELL order generated for " + symbol + " at " + marketData.getBidPrice());
            }
        }
        return orders;
    }
}

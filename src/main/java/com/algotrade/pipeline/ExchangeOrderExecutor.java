package com.algotrade.pipeline;

import com.algotrade.exchange.Exchange;
import com.algotrade.metrics.LatencyMetrics;
import com.algotrade.metrics.TradeMetrics;
import com.algotrade.model.Order;
import com.algotrade.model.Trade;
import com.algotrade.risk.PositionManager;

import java.util.List;

public class ExchangeOrderExecutor implements OrderExecutor {
    private final Exchange exchange;
    private final PositionManager positionManager;
    private final TradeMetrics tradeMetrics;
    private final LatencyMetrics latencyMetrics;

    public ExchangeOrderExecutor(Exchange exchange, PositionManager positionManager, TradeMetrics tradeMetrics, LatencyMetrics latencyMetrics) {
        this.exchange = exchange;
        this.positionManager = positionManager;
        this.tradeMetrics = tradeMetrics;
        this.latencyMetrics = latencyMetrics;
    }

    @Override
    public List<Trade> executeOrder(Order order) {
        tradeMetrics.recordOrder(order);
        List<Trade> trades = exchange.placeOrder(order);
        if (!trades.isEmpty()) {
            positionManager.updatePosition(order);
            trades.forEach(trade -> {
                tradeMetrics.recordTrade(trade);
                latencyMetrics.recordTradeExecution(trade);
            });
        }
        return trades;
    }
}

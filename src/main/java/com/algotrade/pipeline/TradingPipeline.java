package com.algotrade.pipeline;

import com.algotrade.metrics.LatencyMetrics;
import com.algotrade.metrics.TradeMetrics;
import com.algotrade.model.MarketData;
import com.algotrade.model.Order;
import com.algotrade.model.Trade;
import com.algotrade.exchange.Exchange;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TradingPipeline implements MarketDataProcessor {
    private final StrategyProcessor strategyProcessor;
    private final RiskManager riskManager;
    private final OrderExecutor orderExecutor;
    private final Exchange exchange;
    private final TradeMetrics tradeMetrics;
    private final LatencyMetrics latencyMetrics;
    private final ExecutorService marketDataExecutor;
    private final ExecutorService strategyExecutor;
    private final ExecutorService riskExecutor;
    private final ExecutorService executionExecutor;

    public TradingPipeline(StrategyProcessor strategyProcessor, RiskManager riskManager, OrderExecutor orderExecutor, Exchange exchange, TradeMetrics tradeMetrics, LatencyMetrics latencyMetrics) {
        this.strategyProcessor = strategyProcessor;
        this.riskManager = riskManager;
        this.orderExecutor = orderExecutor;
        this.exchange = exchange;
        this.tradeMetrics = tradeMetrics;
        this.latencyMetrics = latencyMetrics;

        this.marketDataExecutor = Executors.newSingleThreadExecutor();
        this.strategyExecutor = Executors.newSingleThreadExecutor();
        this.riskExecutor = Executors.newSingleThreadExecutor();
        this.executionExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void processMarketData(MarketData marketData) {
        marketDataExecutor.submit(() -> {
            System.out.println("Processing market data: " + marketData);
            // 1. Strategy
            Future<List<Order>> ordersFuture = strategyExecutor.submit(() -> strategyProcessor.processMarketData(marketData));
            try {
                List<Order> orders = ordersFuture.get();
                for (Order order : orders) {
                    // 2. Risk Check
                    Future<Boolean> riskCheckFuture = riskExecutor.submit(() -> riskManager.checkOrder(order));
                    if (riskCheckFuture.get()) {
                        latencyMetrics.recordOrderCreation(order);
                        // 3. Execution
                        executionExecutor.submit(() -> {
                            List<Trade> trades = orderExecutor.executeOrder(order);
                            trades.forEach(trade -> System.out.println("Executed Trade: " + trade));
                        });
                    } else {
                        System.out.println("Order rejected by risk manager: " + order);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in pipeline: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        marketDataExecutor.shutdown();
        strategyExecutor.shutdown();
        riskExecutor.shutdown();
        executionExecutor.shutdown();
    }
}

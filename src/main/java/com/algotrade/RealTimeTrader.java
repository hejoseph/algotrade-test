package com.algotrade;

import com.algotrade.exchange.Exchange;
import com.algotrade.metrics.LatencyMetrics;
import com.algotrade.metrics.TradeMetrics;
import com.algotrade.model.MarketData;
import com.algotrade.pipeline.*;
import com.algotrade.risk.MaxPositionRiskManager;
import com.algotrade.risk.PositionManager;
import com.algotrade.simulator.LiveMarketDataProvider;
import com.algotrade.strategy.MeanReversionStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point for real-time trading simulation using live Binance data.
 * Assembles pipeline and runs indefinitely until interrupted.
 */
public class RealTimeTrader {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Initializing Real-Time Trading Simulation ---");

        String symbol = "BTC/USD";  // Normalized to BTCUSDT for Binance
        Exchange exchange = new Exchange();
        exchange.addSymbol(symbol);

        TradeMetrics tradeMetrics = new TradeMetrics();
        LatencyMetrics latencyMetrics = new LatencyMetrics();

        // Strategy
        int lookbackPeriod = 50;
        double priceThreshold = 0.001;
        long orderQuantity = 1;
        MeanReversionStrategy strategy = new MeanReversionStrategy(symbol, lookbackPeriod, priceThreshold, orderQuantity);

        // Risk
        PositionManager positionManager = new PositionManager();
        MaxPositionRiskManager riskManager = new MaxPositionRiskManager(positionManager, symbol, 10L);

        // Execution
        OrderExecutor delegateExecutor = new ExchangeOrderExecutor(exchange, positionManager, tradeMetrics, latencyMetrics);
        OrderExecutor orderExecutor = new ExecutionThrottler(delegateExecutor, 5, 1000);  // 5/sec

        // Pipeline
        TradingPipeline pipeline = new TradingPipeline(strategy, riskManager, orderExecutor, exchange, tradeMetrics, latencyMetrics);

        // Live data provider
        LiveMarketDataProvider liveProvider = new LiveMarketDataProvider(symbol, pipeline);
        ExecutorService dataExecutor = Executors.newSingleThreadExecutor();
        dataExecutor.submit(liveProvider);

        System.out.println("Real-time simulation running. Press Ctrl+C to stop.");

        // Block until interrupted (Ctrl+C)
        Thread.sleep(Long.MAX_VALUE);
    }
}
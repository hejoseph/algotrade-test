package com.algotrade;

import com.algotrade.exchange.Exchange;
import com.algotrade.metrics.LatencyMetrics;
import com.algotrade.metrics.TradeMetrics;
import com.algotrade.pipeline.*;
import com.algotrade.risk.MaxPositionRiskManager;
import com.algotrade.risk.PositionManager;
import com.algotrade.simulator.LiveMarketDataProvider;
import com.algotrade.strategy.MeanReversionStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entry point for real-time trading simulation using live Binance data.
 * Assembles pipeline and runs indefinitely until interrupted (Ctrl+C).
 * Prints final metrics on shutdown.
 */
public class RealTimeTrader {
    private static TradingPipeline pipeline;
    private static LiveMarketDataProvider liveProvider;
    private static ExecutorService dataExecutor;
    private static TradeMetrics tradeMetrics;
    private static PositionManager positionManager;
    private static String symbol;

    public static void main(String[] args) {
        System.out.println("--- Initializing Real-Time Trading Simulation ---");

        symbol = "BTCUSDT";  // Binance symbol
        Exchange exchange = new Exchange();
        exchange.addSymbol(symbol);

        tradeMetrics = new TradeMetrics();
        LatencyMetrics latencyMetrics = new LatencyMetrics();

        // Strategy
        int lookbackPeriod = 50;
        double priceThreshold = 0.001;
        long orderQuantity = 1;
        MeanReversionStrategy strategy = new MeanReversionStrategy(symbol, lookbackPeriod, priceThreshold, orderQuantity);

        // Risk
        positionManager = new PositionManager();
        MaxPositionRiskManager riskManager = new MaxPositionRiskManager(positionManager, symbol, 10L);

        // Execution
        OrderExecutor delegateExecutor = new ExchangeOrderExecutor(exchange, positionManager, tradeMetrics, latencyMetrics);
        OrderExecutor orderExecutor = new ExecutionThrottler(delegateExecutor, 5, 1000);  // 5/sec

        // Pipeline
        pipeline = new TradingPipeline(strategy, riskManager, orderExecutor, exchange, tradeMetrics, latencyMetrics);

        // Live data provider
        liveProvider = new LiveMarketDataProvider(symbol, pipeline);
        dataExecutor = Executors.newSingleThreadExecutor();
        dataExecutor.submit(liveProvider);

        System.out.println("Real-time simulation running for " + symbol + ". Press Ctrl+C to stop.");

        // Shutdown hook for graceful exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown initiated...");
            if (liveProvider != null) liveProvider.shutdown();
            if (dataExecutor != null) dataExecutor.shutdownNow();
            if (pipeline != null) pipeline.shutdown();
            System.out.println("---- Final Metrics ----");
            System.out.println("PnL for " + symbol + ": " + tradeMetrics.getPnl(symbol));
            System.out.println("Fill Ratio for " + symbol + ": " + tradeMetrics.getFillRatio(symbol));
            System.out.println("Final Position for " + symbol + ": " + positionManager.getPosition(symbol));
        }));

        // Block until interrupted
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Shutdown hook will handle cleanup
        }
    }
}
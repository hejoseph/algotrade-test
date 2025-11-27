package com.algotrade.simulator;

import com.algotrade.exchange.Exchange;
import com.algotrade.metrics.LatencyMetrics;
import com.algotrade.metrics.TradeMetrics;
import com.algotrade.pipeline.ExchangeOrderExecutor;
import com.algotrade.pipeline.ExecutionThrottler;
import com.algotrade.pipeline.MarketDataProcessor;
import com.algotrade.pipeline.TradingPipeline;
import com.algotrade.risk.MaxPositionRiskManager;
import com.algotrade.risk.PositionManager;
import com.algotrade.strategy.MeanReversionStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Backtester {
    private final String symbol;
    private final double initialPrice;
    private final int lookbackPeriod;
    private final double priceThreshold;
    private final long orderQuantity;
    private final long maxAbsolutePosition;
    private final int throttlePermits;
    private final long throttleIntervalMillis;
    private final long marketDataIntervalMillis;
    private final long durationSeconds;

    private Exchange exchange;
    private PositionManager positionManager;
    private TradeMetrics tradeMetrics;
    private LatencyMetrics latencyMetrics;
    private TradingPipeline tradingPipeline;
    private MarketDataGenerator marketDataGenerator;
    private ExecutorService marketDataExecutorService;

    public Backtester(String symbol, double initialPrice, int lookbackPeriod, double priceThreshold, long orderQuantity, long maxAbsolutePosition, int throttlePermits, long throttleIntervalMillis, long marketDataIntervalMillis, long durationSeconds) {
        this.symbol = symbol;
        this.initialPrice = initialPrice;
        this.lookbackPeriod = lookbackPeriod;
        this.priceThreshold = priceThreshold;
        this.orderQuantity = orderQuantity;
        this.maxAbsolutePosition = maxAbsolutePosition;
        this.throttlePermits = throttlePermits;
        this.throttleIntervalMillis = throttleIntervalMillis;
        this.marketDataIntervalMillis = marketDataIntervalMillis;
        this.durationSeconds = durationSeconds;
    }

    public void runBacktest() throws InterruptedException {
        System.out.println("Starting backtest for " + symbol + "...");

        // 1. Initialize Components
        exchange = new Exchange();
        exchange.addSymbol(symbol);
        positionManager = new PositionManager();
        tradeMetrics = new TradeMetrics();
        latencyMetrics = new LatencyMetrics();

        MeanReversionStrategy strategy = new MeanReversionStrategy(symbol, lookbackPeriod, priceThreshold, orderQuantity);
        MaxPositionRiskManager riskManager = new MaxPositionRiskManager(positionManager, symbol, maxAbsolutePosition);
        ExchangeOrderExecutor rawOrderExecutor = new ExchangeOrderExecutor(exchange, positionManager, tradeMetrics, latencyMetrics);
        ExecutionThrottler throttledOrderExecutor = new ExecutionThrottler(rawOrderExecutor, throttlePermits, throttleIntervalMillis);

        tradingPipeline = new TradingPipeline(strategy, riskManager, throttledOrderExecutor, exchange, tradeMetrics, latencyMetrics);

        // 2. Market Data Generation
        marketDataGenerator = new MarketDataGenerator(symbol, marketDataIntervalMillis, tradingPipeline::processMarketData, initialPrice);
        marketDataExecutorService = Executors.newSingleThreadExecutor();
        marketDataExecutorService.submit(marketDataGenerator);

        // 3. Run for specified duration
        TimeUnit.SECONDS.sleep(durationSeconds);

        // 4. Shutdown and Report
        marketDataGenerator.stop();
        marketDataExecutorService.shutdownNow();
        tradingPipeline.shutdown();

        System.out.println("Backtest finished for " + symbol + ".");
        System.out.println("---- Metrics ----");
        System.out.println("Final PnL for " + symbol + ": " + tradeMetrics.getPnl(symbol));
        System.out.println("Fill Ratio for " + symbol + ": " + tradeMetrics.getFillRatio(symbol));
        System.out.println("Final Position for " + symbol + ": " + positionManager.getPosition(symbol));
    }

    public static void main(String[] args) throws InterruptedException {
        Backtester backtester = new Backtester(
                "BTC/USD",          // symbol
                60000.0,            // initialPrice
                50,                 // lookbackPeriod (for MeanReversionStrategy)
                0.001,              // priceThreshold (for MeanReversionStrategy)
                1,                  // orderQuantity
                10,                 // maxAbsolutePosition (for MaxPositionRiskManager)
                5,                  // throttlePermits (for ExecutionThrottler)
                1000,               // throttleIntervalMillis (for ExecutionThrottler)
                100,                // marketDataIntervalMillis (for MarketDataGenerator)
                60                  // durationSeconds
        );
        backtester.runBacktest();
    }
}

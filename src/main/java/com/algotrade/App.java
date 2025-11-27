package com.algotrade;

import com.algotrade.exchange.Exchange;
import com.algotrade.metrics.LatencyMetrics;
import com.algotrade.metrics.TradeMetrics;
import com.algotrade.model.MarketData;
import com.algotrade.pipeline.ExchangeOrderExecutor;
import com.algotrade.pipeline.ExecutionThrottler;
import com.algotrade.pipeline.MarketDataStream;
import com.algotrade.pipeline.TradingPipeline;
import com.algotrade.risk.MaxPositionRiskManager;
import com.algotrade.risk.PositionManager;
import com.algotrade.simulator.MarketDataGenerator;
import com.algotrade.strategy.MeanReversionStrategy;

import java.util.concurrent.TimeUnit;

/**
 * Main application to run the algorithmic trading engine simulation.
 */
public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Initializing Simulation Environment ---");
        Exchange exchange = new Exchange();
        TradeMetrics tradeMetrics = new TradeMetrics();
        LatencyMetrics latencyMetrics = new LatencyMetrics();

        String symbol = "TESTINGSYM";
        int lookbackPeriod = 10;
        double priceThreshold = 0.001;
        long orderQuantity = 100;
        MeanReversionStrategy meanReversionStrategy = new MeanReversionStrategy(symbol, lookbackPeriod, priceThreshold, orderQuantity);

        System.out.println("MeanReversionStrategy configured.");
    }
}

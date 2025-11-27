package com.algotrade;

import com.algotrade.exchange.Exchange;
import com.algotrade.metrics.LatencyMetrics;
import com.algotrade.metrics.TradeMetrics;
import com.algotrade.model.MarketData;
import com.algotrade.pipeline.*;
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


        //Configure and instantiate the trading strategy
        String symbol = "TESTINGSYM";
        int lookbackPeriod = 10;
        double priceThreshold = 0.001;
        long orderQuantity = 100;
        MeanReversionStrategy meanReversionStrategy = new MeanReversionStrategy(symbol, lookbackPeriod, priceThreshold, orderQuantity);

        // --- 3. Configure and instantiate risk management and execution components ---
        PositionManager positionManager = new PositionManager();
        MaxPositionRiskManager riskManager = new MaxPositionRiskManager(positionManager, symbol, 500L); // Max position of 500 for TESTINGSYM

        // The actual executor that sends orders to the exchange
        OrderExecutor delegateExecutor = new ExchangeOrderExecutor(exchange, positionManager, tradeMetrics, latencyMetrics);

        // The throttler wraps the actual executor to control the rate of orders
        OrderExecutor orderExecutor = new ExecutionThrottler(delegateExecutor, 5, 1000); // 5 orders per 1 second

        System.out.println("Risk management and execution components configured.");
    }
}

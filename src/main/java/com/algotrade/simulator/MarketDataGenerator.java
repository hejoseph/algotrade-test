package com.algotrade.simulator;

import com.algotrade.model.MarketData;

import java.util.Random;
import java.util.function.Consumer;

public class MarketDataGenerator implements Runnable {
    private final String symbol;
    private final long intervalMillis;
    private final Consumer<MarketData> marketDataConsumer;
    private final Random random;
    private volatile boolean running = true;

    private double currentBid;
    private double currentAsk;

    public MarketDataGenerator(String symbol, long intervalMillis, Consumer<MarketData> marketDataConsumer, double initialPrice) {
        this.symbol = symbol;
        this.intervalMillis = intervalMillis;
        this.marketDataConsumer = marketDataConsumer;
        this.random = new Random();
        this.currentBid = initialPrice - 0.01; // Initial bid slightly below initial price
        this.currentAsk = initialPrice + 0.01; // Initial ask slightly above initial price
    }

    @Override
    public void run() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Simulate price fluctuations
                currentBid += (random.nextDouble() - 0.5) * 0.1; // Small random changes
                currentAsk = currentBid + (0.01 + random.nextDouble() * 0.05); // Spread between 0.01 and 0.06

                // Ensure prices don't go negative
                currentBid = Math.max(0.01, currentBid);
                currentAsk = Math.max(0.02, currentAsk);

                long bidQuantity = 100 + random.nextInt(500);
                long askQuantity = 100 + random.nextInt(500);

                MarketData marketData = new MarketData(symbol, currentBid, currentAsk, bidQuantity, askQuantity);
                marketDataConsumer.accept(marketData);

                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("MarketDataGenerator for " + symbol + " interrupted.");
                break;
            }
        }
    }

    public void stop() {
        running = false;
    }
}

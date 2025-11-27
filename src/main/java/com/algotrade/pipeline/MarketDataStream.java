package com.algotrade.pipeline;

import com.algotrade.model.MarketData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class MarketDataStream {
    private final ExecutorService executorService;
    private final Consumer<MarketData> marketDataHandler;

    public MarketDataStream(Consumer<MarketData> marketDataHandler) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.marketDataHandler = marketDataHandler;
    }

    public void publishMarketData(MarketData marketData) {
        executorService.submit(() -> {
            marketDataHandler.accept(marketData);
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

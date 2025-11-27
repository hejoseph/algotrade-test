package com.algotrade.pipeline;

import com.algotrade.model.MarketData;

public interface MarketDataProcessor {
    void processMarketData(MarketData marketData);
}

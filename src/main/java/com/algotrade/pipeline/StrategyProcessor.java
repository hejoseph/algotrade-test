package com.algotrade.pipeline;

import com.algotrade.model.MarketData;
import com.algotrade.model.Order;

import java.util.List;

public interface StrategyProcessor {
    List<Order> processMarketData(MarketData marketData);
}

package com.algotrade.strategy;

import com.algotrade.model.MarketData;
import com.algotrade.model.Order;
import com.algotrade.model.OrderType;
import com.algotrade.model.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeanReversionStrategyTest {

    private MeanReversionStrategy strategy;
    private final String SYMBOL = "TESTSYM";
    private final int LOOKBACK_PERIOD = 3;
    private final double PRICE_THRESHOLD = 0.01; // 1%
    private final long ORDER_QUANTITY = 10;

    @BeforeEach
    void setUp() {
        strategy = new MeanReversionStrategy(SYMBOL, LOOKBACK_PERIOD, PRICE_THRESHOLD, ORDER_QUANTITY);
    }

    @Test
    void testNoOrderBeforeLookbackPeriodFilled() {
        // Not enough data points
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100));
        List<Order> orders = strategy.processMarketData(new MarketData(SYMBOL, 99.5, 100.5, 100, 100));
        assertTrue(orders.isEmpty());
    }

    @Test
    void testBuyOrderGeneratedWhenPriceIsLow() {
        // Fill history with prices around 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100. Avg: 100

        // Price drops significantly below average (100 * (1 - 0.01) = 99)
        List<Order> orders = strategy.processMarketData(new MarketData(SYMBOL, 97.0, 98.0, 100, 100)); // Ask: 98.0 < 99.0

        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertEquals(Side.BUY, orders.get(0).getSide());
        assertEquals(OrderType.LIMIT, orders.get(0).getOrderType());
        assertEquals(98.0, orders.get(0).getPrice());
        assertEquals(ORDER_QUANTITY, orders.get(0).getQuantity());
    }

    @Test
    void testSellOrderGeneratedWhenPriceIsHigh() {
        // Fill history with prices around 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100. Avg: 100

        // Price rises significantly above average (100 * (1 + 0.01) = 101)
        List<Order> orders = strategy.processMarketData(new MarketData(SYMBOL, 102.0, 103.0, 100, 100)); // Bid: 102.0 > 101.0

        assertFalse(orders.isEmpty());
        assertEquals(1, orders.size());
        assertEquals(Side.SELL, orders.get(0).getSide());
        assertEquals(OrderType.LIMIT, orders.get(0).getOrderType());
        assertEquals(102.0, orders.get(0).getPrice());
        assertEquals(ORDER_QUANTITY, orders.get(0).getQuantity());
    }

    @Test
    void testNoOrderWhenPriceIsWithinThreshold() {
        // Fill history with prices around 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100)); // Mid: 100. Avg: 100

        // Price is within the threshold (e.g., ask 99.5, bid 100.5, avg 100, threshold 1%)
        List<Order> orders = strategy.processMarketData(new MarketData(SYMBOL, 98.5, 100.5, 100, 100)); // Ask 100.5 > 99.0, Bid 98.5 < 101.0

        assertTrue(orders.isEmpty());
    }

    @Test
    void testStrategyIgnoresOtherSymbols() {
        // Fill history with prices around 100
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100));
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100));
        strategy.processMarketData(new MarketData(SYMBOL, 99.0, 101.0, 100, 100));

        // Provide market data for a different symbol
        List<Order> orders = strategy.processMarketData(new MarketData("OTHER", 97.0, 98.0, 100, 100));
        assertTrue(orders.isEmpty());
    }
}

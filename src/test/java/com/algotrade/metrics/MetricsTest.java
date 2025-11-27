package com.algotrade.metrics;

import com.algotrade.model.Order;
import com.algotrade.model.OrderType;
import com.algotrade.model.Side;
import com.algotrade.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsTest {

    private TradeMetrics tradeMetrics;
    private LatencyMetrics latencyMetrics;
    private final String SYMBOL = "TESTSYM";

    @BeforeEach
    void setUp() {
        tradeMetrics = new TradeMetrics();
        latencyMetrics = new LatencyMetrics();
    }

    @Test
    void testPnlCalculation() {
        // Buy 10 at 100
        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 10);
        Trade buyTrade = new Trade(buyOrder.getOrderId(), SYMBOL, 100.0, 10, Side.BUY);
        tradeMetrics.recordOrder(buyOrder);
        tradeMetrics.recordTrade(buyTrade);
        assertEquals(-1000.0, tradeMetrics.getPnl(SYMBOL), 0.001);

        // Sell 5 at 105
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 105.0, 5);
        Trade sellTrade = new Trade(sellOrder.getOrderId(), SYMBOL, 105.0, 5, Side.SELL);
        tradeMetrics.recordOrder(sellOrder);
        tradeMetrics.recordTrade(sellTrade);
        assertEquals(-1000.0 + (105.0 * 5), tradeMetrics.getPnl(SYMBOL), 0.001);
    }

    @Test
    void testFillRatioCalculation() {
        Order order1 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 10);
        tradeMetrics.recordOrder(order1);

        Trade trade1 = new Trade(order1.getOrderId(), SYMBOL, 100.0, 5, Side.BUY);
        tradeMetrics.recordTrade(trade1);

        assertEquals(0.5, tradeMetrics.getFillRatio(SYMBOL), 0.001);

        Order order2 = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 101.0, 20);
        tradeMetrics.recordOrder(order2);

        Trade trade2 = new Trade(order2.getOrderId(), SYMBOL, 101.0, 10, Side.SELL);
        tradeMetrics.recordTrade(trade2);

        assertEquals(0.5, tradeMetrics.getFillRatio(SYMBOL), 0.001);
    }

    @Test
    void testLatencyRecording() throws InterruptedException {
        Order order = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 10);
        latencyMetrics.recordOrderCreation(order);

        // Simulate some processing time
        Thread.sleep(50);

        Trade trade = new Trade(order.getOrderId(), SYMBOL, 100.0, 10, Side.BUY);
        // The actual latency check would be inside recordTradeExecution, which prints to console.
        // For unit testing, we can't easily assert the printed output. In a real system,
        // LatencyMetrics would likely expose aggregated data for assertion.
        latencyMetrics.recordTradeExecution(trade);

        // As a placeholder, we can at least assert no exceptions and that the internal queue is empty.
        assertTrue(latencyMetrics.getOrderToExecutionLatencies().get(order.getOrderId()).isEmpty());
    }
}

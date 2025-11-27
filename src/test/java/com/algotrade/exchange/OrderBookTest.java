package com.algotrade.exchange;

import com.algotrade.model.Order;
import com.algotrade.model.OrderType;
import com.algotrade.model.Side;
import com.algotrade.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    private OrderBook orderBook;
    private final String SYMBOL = "TESTSYM";

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook(SYMBOL);
    }

    @Test
    void testBuyLimitOrderMatchingSellLimitOrder() {
        // Place a sell limit order first
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 10);
        orderBook.processOrder(sellOrder);

        // Place a buy limit order that matches
        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 5);
        List<Trade> trades = orderBook.processOrder(buyOrder);

        assertFalse(trades.isEmpty());
        assertEquals(1, trades.size());
        assertEquals(5, trades.get(0).getQuantity());
        assertEquals(100.0, trades.get(0).getPrice());
        assertEquals(5, orderBook.getSellOrders().peek().getQuantity()); // Remaining sell quantity
        assertTrue(orderBook.getBuyOrders().isEmpty());
    }

    @Test
    void testSellLimitOrderMatchingBuyLimitOrder() {
        // Place a buy limit order first
        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 10);
        orderBook.processOrder(buyOrder);

        // Place a sell limit order that matches
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 5);
        List<Trade> trades = orderBook.processOrder(sellOrder);

        assertFalse(trades.isEmpty());
        assertEquals(1, trades.size());
        assertEquals(5, trades.get(0).getQuantity());
        assertEquals(100.0, trades.get(0).getPrice());
        assertEquals(5, orderBook.getBuyOrders().peek().getQuantity()); // Remaining buy quantity
        assertTrue(orderBook.getSellOrders().isEmpty());
    }

    @Test
    void testBuyMarketOrderMatchingSellLimitOrder() {
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 10);
        orderBook.processOrder(sellOrder);

        Order marketBuyOrder = new Order(SYMBOL, OrderType.MARKET, Side.BUY, 0.0, 7);
        List<Trade> trades = orderBook.processOrder(marketBuyOrder);

        assertFalse(trades.isEmpty());
        assertEquals(1, trades.size());
        assertEquals(7, trades.get(0).getQuantity());
        assertEquals(100.0, trades.get(0).getPrice());
        assertEquals(3, orderBook.getSellOrders().peek().getQuantity());
        assertTrue(orderBook.getBuyOrders().isEmpty());
    }

    @Test
    void testSellMarketOrderMatchingBuyLimitOrder() {
        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 10);
        orderBook.processOrder(buyOrder);

        Order marketSellOrder = new Order(SYMBOL, OrderType.MARKET, Side.SELL, 0.0, 7);
        List<Trade> trades = orderBook.processOrder(marketSellOrder);

        assertFalse(trades.isEmpty());
        assertEquals(1, trades.size());
        assertEquals(7, trades.get(0).getQuantity());
        assertEquals(100.0, trades.get(0).getPrice());
        assertEquals(3, orderBook.getBuyOrders().peek().getQuantity());
        assertTrue(orderBook.getSellOrders().isEmpty());
    }

    @Test
    void testNoMatchForLimitOrders() {
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 101.0, 10);
        orderBook.processOrder(sellOrder);

        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 5);
        List<Trade> trades = orderBook.processOrder(buyOrder);

        assertTrue(trades.isEmpty());
        assertEquals(1, orderBook.getSellOrders().size());
        assertEquals(1, orderBook.getBuyOrders().size());
    }

    @Test
    void testMultipleMatches() {
        // Setup multiple sell orders
        orderBook.processOrder(new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 5));
        orderBook.processOrder(new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.5, 5));

        // Place a buy order that crosses both
        Order buyOrder = new Order(SYMBOL, OrderType.MARKET, Side.BUY, 0.0, 10);
        List<Trade> trades = orderBook.processOrder(buyOrder);

        assertEquals(2, trades.size());
        assertEquals(0, orderBook.getSellOrders().size());
        assertTrue(orderBook.getBuyOrders().isEmpty());

        // Verify trades
        assertEquals(5, trades.get(0).getQuantity());
        assertEquals(100.0, trades.get(0).getPrice());
        assertEquals(5, trades.get(1).getQuantity());
        assertEquals(100.5, trades.get(1).getPrice());
    }

    @Test
    void testPartialFillAndRemainingOrder() {
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 10);
        orderBook.processOrder(sellOrder);

        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 15);
        List<Trade> trades = orderBook.processOrder(buyOrder);

        assertFalse(trades.isEmpty());
        assertEquals(1, trades.size());
        assertEquals(10, trades.get(0).getQuantity()); // All sell order quantity filled
        assertEquals(100.0, trades.get(0).getPrice());
        assertTrue(orderBook.getSellOrders().isEmpty());
        assertEquals(1, orderBook.getBuyOrders().size()); // Remaining buy order
        assertEquals(5, orderBook.getBuyOrders().peek().getQuantity());
    }

}

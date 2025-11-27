package com.algotrade.risk;

import com.algotrade.model.Order;
import com.algotrade.model.OrderType;
import com.algotrade.model.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaxPositionRiskManagerTest {

    private PositionManager positionManager;
    private MaxPositionRiskManager riskManager;
    private final String SYMBOL = "TESTSYM";
    private final long MAX_POSITION = 10;

    @BeforeEach
    void setUp() {
        positionManager = new PositionManager();
        riskManager = new MaxPositionRiskManager(positionManager, SYMBOL, MAX_POSITION);
    }

    @Test
    void testBuyOrderWithinMaxPosition() {
        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 5);
        assertTrue(riskManager.checkOrder(buyOrder));
    }

    @Test
    void testBuyOrderExceedsMaxPosition() {
        positionManager.updatePosition(new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 7));
        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 4); // 7 + 4 = 11, exceeds 10
        assertFalse(riskManager.checkOrder(buyOrder));
    }

    @Test
    void testSellOrderWithinMaxPosition() {
        positionManager.updatePosition(new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 7)); // Current position +7
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 5); // 7 - 5 = 2, within -10 to +10
        assertTrue(riskManager.checkOrder(sellOrder));
    }

    @Test
    void testSellOrderExceedsMaxPosition() {
        positionManager.updatePosition(new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 7)); // Current position -7
        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 4); // -7 - 4 = -11, exceeds -10
        assertFalse(riskManager.checkOrder(sellOrder));
    }

    @Test
    void testOrderForDifferentSymbolIsApproved() {
        Order buyOrder = new Order("ANOTHER_SYM", OrderType.LIMIT, Side.BUY, 100.0, 15);
        assertTrue(riskManager.checkOrder(buyOrder)); // Should be approved as it's not the managed symbol
    }

    @Test
    void testZeroPositionCheck() {
        Order buyOrder = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 10);
        assertTrue(riskManager.checkOrder(buyOrder));

        positionManager.updatePosition(buyOrder);
        assertEquals(10, positionManager.getPosition(SYMBOL));

        Order sellOrder = new Order(SYMBOL, OrderType.LIMIT, Side.SELL, 100.0, 20); // -10 final position, exceeds max absolute 10
        assertFalse(riskManager.checkOrder(sellOrder));
    }
}

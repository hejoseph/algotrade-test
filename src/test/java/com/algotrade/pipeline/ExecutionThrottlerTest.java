package com.algotrade.pipeline;

import com.algotrade.model.Order;
import com.algotrade.model.OrderType;
import com.algotrade.model.Side;
import com.algotrade.model.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionThrottlerTest {

    @Mock
    private OrderExecutor mockDelegateExecutor;

    private ExecutionThrottler throttler;
    private final String SYMBOL = "TESTSYM";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testOrderExecutionWithinRateLimit() throws InterruptedException {
        // Allow 2 permits per 100ms
        throttler = new ExecutionThrottler(mockDelegateExecutor, 2, 100);
        when(mockDelegateExecutor.executeOrder(any(Order.class))).thenReturn(List.of(new Trade("trade1", SYMBOL, 100.0, 1, Side.BUY)));

        Order order1 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);
        Order order2 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);

        List<Trade> trades1 = throttler.executeOrder(order1);
        List<Trade> trades2 = throttler.executeOrder(order2);

        assertFalse(trades1.isEmpty());
        assertFalse(trades2.isEmpty());
        verify(mockDelegateExecutor, times(1)).executeOrder(order1);
        verify(mockDelegateExecutor, times(1)).executeOrder(order2);

        // Try to execute a third order immediately, it should be throttled
        Order order3 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);
        List<Trade> trades3 = throttler.executeOrder(order3);
        assertTrue(trades3.isEmpty());
        verify(mockDelegateExecutor, never()).executeOrder(order3);

        // Wait for the throttle interval to reset
        TimeUnit.MILLISECONDS.sleep(150);

        // Now the third order should go through
        trades3 = throttler.executeOrder(order3);
        assertFalse(trades3.isEmpty());
        verify(mockDelegateExecutor, times(1)).executeOrder(order3);
    }

    @Test
    void testOrderThrottling() throws InterruptedException {
        // Allow 1 permit per 200ms
        throttler = new ExecutionThrottler(mockDelegateExecutor, 1, 200);
        when(mockDelegateExecutor.executeOrder(any(Order.class))).thenReturn(List.of(new Trade("trade1", SYMBOL, 100.0, 1, Side.BUY)));

        Order order1 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);
        Order order2 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);

        List<Trade> trades1 = throttler.executeOrder(order1);
        List<Trade> trades2 = throttler.executeOrder(order2);

        assertFalse(trades1.isEmpty());
        assertTrue(trades2.isEmpty()); // order2 should be throttled
        verify(mockDelegateExecutor, times(1)).executeOrder(order1);
        verify(mockDelegateExecutor, never()).executeOrder(order2);

        // Wait for reset and try again
        TimeUnit.MILLISECONDS.sleep(250);
        trades2 = throttler.executeOrder(order2);
        assertFalse(trades2.isEmpty());
        verify(mockDelegateExecutor, times(1)).executeOrder(order2);
    }

    @Test
    void testMultipleThreadsThrottling() throws InterruptedException {
        throttler = new ExecutionThrottler(mockDelegateExecutor, 2, 500);
        when(mockDelegateExecutor.executeOrder(any(Order.class))).thenReturn(List.of(new Trade("trade1", SYMBOL, 100.0, 1, Side.BUY)));

        AtomicInteger executedOrders = new AtomicInteger(0);
        Runnable task = () -> {
            Order order = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);
            if (!throttler.executeOrder(order).isEmpty()) {
                executedOrders.incrementAndGet();
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            executor.submit(task);
        }

        TimeUnit.MILLISECONDS.sleep(100); // Give some time for initial attempts

        // Expect 2 orders to be executed initially
        assertEquals(2, executedOrders.get());
        verify(mockDelegateExecutor, times(2)).executeOrder(any(Order.class));

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    // Helper for testing the background release of permits
    @Test
    void testPermitReleaseAfterInterval() throws InterruptedException {
        throttler = new ExecutionThrottler(mockDelegateExecutor, 1, 100);
        when(mockDelegateExecutor.executeOrder(any(Order.class))).thenReturn(List.of(new Trade("trade1", SYMBOL, 100.0, 1, Side.BUY)));

        Order order1 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);
        throttler.executeOrder(order1);
        verify(mockDelegateExecutor, times(1)).executeOrder(order1);

        Order order2 = new Order(SYMBOL, OrderType.LIMIT, Side.BUY, 100.0, 1);
        assertTrue(throttler.executeOrder(order2).isEmpty()); // Throttled
        verify(mockDelegateExecutor, never()).executeOrder(order2);

        TimeUnit.MILLISECONDS.sleep(150); // Wait for the interval to pass and permits to release

        assertFalse(throttler.executeOrder(order2).isEmpty()); // Should now execute
        verify(mockDelegateExecutor, times(1)).executeOrder(order2);
    }
}

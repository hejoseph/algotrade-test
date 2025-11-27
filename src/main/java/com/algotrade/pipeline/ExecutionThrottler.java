package com.algotrade.pipeline;

import com.algotrade.model.Order;
import com.algotrade.model.Trade;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ExecutionThrottler implements OrderExecutor {
    private final OrderExecutor delegateExecutor;
    private final Semaphore semaphore;
    private final long rateLimitIntervalMillis;

    public ExecutionThrottler(OrderExecutor delegateExecutor, int permits, long rateLimitIntervalMillis) {
        this.delegateExecutor = delegateExecutor;
        this.semaphore = new Semaphore(permits);
        this.rateLimitIntervalMillis = rateLimitIntervalMillis;

        // A simple background thread to release permits periodically
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(rateLimitIntervalMillis);
                    semaphore.release(permits - semaphore.availablePermits()); // Release all permits
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public List<Trade> executeOrder(Order order) {
        try {
            if (semaphore.tryAcquire(rateLimitIntervalMillis, TimeUnit.MILLISECONDS)) {
                return delegateExecutor.executeOrder(order);
            } else {
                System.out.println("Order throttled: " + order.getOrderId());
                return List.of(); // Return empty list for throttled orders
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }
}

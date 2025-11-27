package com.algotrade.metrics;

import com.algotrade.model.Order;
import com.algotrade.model.Trade;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.Queue;

public class LatencyMetrics {
    private final Map<String, Queue<Long>> orderToExecutionLatencies;

    public LatencyMetrics() {
        this.orderToExecutionLatencies = new ConcurrentHashMap<>();
    }

    public void recordOrderCreation(Order order) {
        orderToExecutionLatencies.computeIfAbsent(order.getOrderId(), k -> new ConcurrentLinkedQueue<>()).offer(order.getCreationTimeMillis());
    }

    public void recordTradeExecution(Trade trade) {
        // Assuming tradeId is related to orderId for latency calculation
        // In a real system, you might have a more robust way to link orders to trades
        String orderId = trade.getTradeId(); // This is a simplification; ideally, Trade should reference Order ID
        Queue<Long> latencies = orderToExecutionLatencies.get(orderId);
        if (latencies != null && !latencies.isEmpty()) {
            long creationTime = latencies.poll(); // Get the creation timestamp
            long latency = trade.getExecutionTimeMillis() - creationTime;
            // Store or process this latency value (e.g., calculate average, min, max)
            System.out.println("Latency for order " + orderId + ": " + latency + " ms");
            // For now, we'll just print it. In a real system, you'd aggregate these.
        }
    }

    // In a more complete system, you would add methods to retrieve aggregated latency statistics.
    public Map<String, Queue<Long>> getOrderToExecutionLatencies() {
        return orderToExecutionLatencies;
    }
}

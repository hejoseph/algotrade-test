package com.algotrade.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Order {
    private final String orderId;
    private final String symbol;
    private final OrderType orderType;
    private final Side side;
    private final double price;
    private long quantity;
    private final LocalDateTime timestamp;
    private final long creationTimeMillis;

    public Order(String symbol, OrderType orderType, Side side, double price, long quantity) {
        this.orderId = UUID.randomUUID().toString();
        this.symbol = symbol;
        this.orderType = orderType;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = LocalDateTime.now();
        this.creationTimeMillis = System.currentTimeMillis();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public Side getSide() {
        return side;
    }

    public double getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public long getCreationTimeMillis() {
        return creationTimeMillis;
    }

    public void reduceQuantity(long amount) {
        if (amount > this.quantity) {
            throw new IllegalArgumentException("Cannot reduce quantity by more than the current quantity.");
        }
        this.quantity -= amount;
    }

    @Override
    public String toString() {
        return "Order{" +
               "orderId='" + orderId + "'" +
               ", symbol='" + symbol + "'" +
               ", orderType=" + orderType +
               ", side=" + side +
               ", price=" + price +
               ", quantity=" + quantity +
               ", timestamp=" + timestamp +
               "}";
    }
}

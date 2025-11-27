package com.algotrade.model;

import java.time.LocalDateTime;

public class Trade {
    private final String tradeId;
    private final String symbol;
    private final double price;
    private final long quantity;
    private final Side side;
    private final LocalDateTime timestamp;
    private final long executionTimeMillis;

    public Trade(String tradeId, String symbol, double price, long quantity, Side side) {
        this.tradeId = tradeId;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
        this.timestamp = LocalDateTime.now();
        this.executionTimeMillis = System.currentTimeMillis();
    }

    public String getTradeId() {
        return tradeId;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public long getQuantity() {
        return quantity;
    }

    public Side getSide() {
        return side;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public long getExecutionTimeMillis() {
        return executionTimeMillis;
    }

    @Override
    public String toString() {
        return "Trade{" +
               "tradeId='" + tradeId + "'" +
               ", symbol='" + symbol + "'" +
               ", price=" + price +
               ", quantity=" + quantity +
               ", side=" + side +
               ", timestamp=" + timestamp +
               "}";
    }
}

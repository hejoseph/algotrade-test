package com.algotrade.model;

import java.time.LocalDateTime;

public class MarketData {
    private final String symbol;
    private final double bidPrice;
    private final double askPrice;
    private final long bidQuantity;
    private final long askQuantity;
    private final LocalDateTime timestamp;

    public MarketData(String symbol, double bidPrice, double askPrice, long bidQuantity, long askQuantity) {
        this.symbol = symbol;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.bidQuantity = bidQuantity;
        this.askQuantity = askQuantity;
        this.timestamp = LocalDateTime.now();
    }

    public String getSymbol() {
        return symbol;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public double getAskPrice() {
        return askPrice;
    }

    public long getBidQuantity() {
        return bidQuantity;
    }

    public long getAskQuantity() {
        return askQuantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "MarketData{" +
               "symbol='" + symbol + "'" +
               ", bidPrice=" + bidPrice +
               ", askPrice=" + askPrice +
               ", bidQuantity=" + bidQuantity +
               ", askQuantity=" + askQuantity +
               ", timestamp=" + timestamp +
               "}";
    }
}

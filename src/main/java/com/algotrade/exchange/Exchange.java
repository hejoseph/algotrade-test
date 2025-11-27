package com.algotrade.exchange;

import com.algotrade.model.Order;
import com.algotrade.model.Trade;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Exchange {
    private final Map<String, OrderBook> orderBooks;

    public Exchange() {
        this.orderBooks = new ConcurrentHashMap<>();
    }

    public void addSymbol(String symbol) {
        orderBooks.putIfAbsent(symbol, new OrderBook(symbol));
    }

    public List<Trade> placeOrder(Order order) {
        OrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) {
            throw new IllegalArgumentException("Symbol not supported: " + order.getSymbol());
        }
        return orderBook.processOrder(order);
    }

    // For testing and monitoring
    public OrderBook getOrderBook(String symbol) {
        return orderBooks.get(symbol);
    }
}

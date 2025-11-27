package com.algotrade.exchange;

import com.algotrade.model.Order;
import com.algotrade.model.OrderType;
import com.algotrade.model.Side;
import com.algotrade.model.Trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderBook {
    private final String symbol;
    private final PriorityQueue<Order> buyOrders;
    private final PriorityQueue<Order> sellOrders;
    private final Map<String, Order> activeOrders;
    private final AtomicLong tradeIdCounter;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.buyOrders = new PriorityQueue<>((o1, o2) -> Double.compare(o2.getPrice(), o1.getPrice())); // Max heap for buy orders (highest price first)
        this.sellOrders = new PriorityQueue<>((o1, o2) -> Double.compare(o1.getPrice(), o2.getPrice())); // Min heap for sell orders (lowest price first)
        this.activeOrders = new ConcurrentHashMap<>();
        this.tradeIdCounter = new AtomicLong(0);
    }

    public synchronized List<Trade> processOrder(Order newOrder) {
        List<Trade> trades = new ArrayList<>();
        activeOrders.put(newOrder.getOrderId(), newOrder);

        if (newOrder.getSide() == Side.BUY) {
            trades.addAll(matchBuyOrder(newOrder));
            if (newOrder.getQuantity() > 0) {
                buyOrders.offer(newOrder);
            }
        } else { // SELL side
            trades.addAll(matchSellOrder(newOrder));
            if (newOrder.getQuantity() > 0) {
                sellOrders.offer(newOrder);
            }
        }
        return trades;
    }

    private List<Trade> matchBuyOrder(Order newBuyOrder) {
        List<Trade> trades = new ArrayList<>();
        while (newBuyOrder.getQuantity() > 0 && !sellOrders.isEmpty()) {
            Order bestSellOrder = sellOrders.peek();

            if (newBuyOrder.getOrderType() == OrderType.MARKET || newBuyOrder.getPrice() >= bestSellOrder.getPrice()) {
                long tradedQuantity = Math.min(newBuyOrder.getQuantity(), bestSellOrder.getQuantity());
                double tradePrice = bestSellOrder.getPrice();

                trades.add(new Trade(newBuyOrder.getOrderId(), symbol, tradePrice, tradedQuantity, Side.BUY));

                newBuyOrder.reduceQuantity(tradedQuantity);
                bestSellOrder.reduceQuantity(tradedQuantity);

                if (bestSellOrder.getQuantity() == 0) {
                    sellOrders.poll();
                    activeOrders.remove(bestSellOrder.getOrderId());
                }
            } else {
                break; // No match for limit buy order
            }
        }
        return trades;
    }

    private List<Trade> matchSellOrder(Order newSellOrder) {
        List<Trade> trades = new ArrayList<>();
        while (newSellOrder.getQuantity() > 0 && !buyOrders.isEmpty()) {
            Order bestBuyOrder = buyOrders.peek();

            if (newSellOrder.getOrderType() == OrderType.MARKET || newSellOrder.getPrice() <= bestBuyOrder.getPrice()) {
                long tradedQuantity = Math.min(newSellOrder.getQuantity(), bestBuyOrder.getQuantity());
                double tradePrice = bestBuyOrder.getPrice();

                trades.add(new Trade(newSellOrder.getOrderId(), symbol, tradePrice, tradedQuantity, Side.SELL));

                newSellOrder.reduceQuantity(tradedQuantity);
                bestBuyOrder.reduceQuantity(tradedQuantity);

                if (bestBuyOrder.getQuantity() == 0) {
                    buyOrders.poll();
                    activeOrders.remove(bestBuyOrder.getOrderId());
                }
            } else {
                break; // No match for limit sell order
            }
        }
        return trades;
    }

    // For testing and monitoring
    public PriorityQueue<Order> getBuyOrders() {
        return buyOrders;
    }

    public PriorityQueue<Order> getSellOrders() {
        return sellOrders;
    }

    public Map<String, Order> getActiveOrders() {
        return activeOrders;
    }
}

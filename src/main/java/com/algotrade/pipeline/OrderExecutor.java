package com.algotrade.pipeline;

import com.algotrade.model.Order;
import com.algotrade.model.Trade;

import java.util.List;

public interface OrderExecutor {
    List<Trade> executeOrder(Order order);
}

package com.algotrade.pipeline;

import com.algotrade.model.Order;

public interface RiskManager {
    boolean checkOrder(Order order);
}

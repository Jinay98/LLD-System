package com.lld.realworldexamples.OnlineStockExchange.observer;

import com.lld.realworldexamples.OnlineStockExchange.models.Stock;

public interface StockObserver {
    void update(Stock stock);
}

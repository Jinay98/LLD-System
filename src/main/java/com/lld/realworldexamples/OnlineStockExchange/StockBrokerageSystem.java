package com.lld.realworldexamples.OnlineStockExchange;

import com.lld.realworldexamples.OnlineStockExchange.enums.OrderType;
import com.lld.realworldexamples.OnlineStockExchange.exceptions.InsufficientFundsException;
import com.lld.realworldexamples.OnlineStockExchange.exceptions.InsufficientStockException;
import com.lld.realworldexamples.OnlineStockExchange.models.Account;
import com.lld.realworldexamples.OnlineStockExchange.models.Order;
import com.lld.realworldexamples.OnlineStockExchange.models.Stock;
import com.lld.realworldexamples.OnlineStockExchange.models.Trade;
import com.lld.realworldexamples.OnlineStockExchange.models.User;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StockBrokerageSystem {
    private static volatile StockBrokerageSystem instance;
    private final Map<String, User> users;
    private final Map<String, Stock> stocks;
    private final StockExchange stockExchange;

    private StockBrokerageSystem() {
        this.users = new ConcurrentHashMap<>();
        this.stocks = new ConcurrentHashMap<>();
        this.stockExchange = StockExchange.getInstance();
    }

    public static StockBrokerageSystem getInstance() {
        if (instance == null) {
            synchronized (StockBrokerageSystem.class) {
                if (instance == null) {
                    instance = new StockBrokerageSystem();
                }
            }
        }
        return instance;
    }

    public User registerUser(String name, double initialAmount) {
        User user = new User(name, initialAmount);
        users.put(user.getUserId(), user);
        return user;
    }

    public Stock addStock(String symbol, double initialPrice) {
        Stock stock = new Stock(symbol, initialPrice);
        stocks.put(stock.getSymbol(), stock);
        return stock;
    }

    public void placeBuyOrder(Order order) {
        Account account = order.getUser().getAccount();
        // A limit buy can be pre-authorized; a market buy is validated by the exchange at execution.
        double estimatedCost = order.getQuantity() * order.getPrice();
        if (order.getType() == OrderType.LIMIT && account.getBalance() < estimatedCost) {
            throw new InsufficientFundsException("Not enough cash to place limit buy order.");
        }
        System.out.printf("Placing BUY order %s for %d shares of %s.%n", order.getOrderId(), order.getQuantity(), order.getStock().getSymbol());
        stockExchange.placeBuyOrder(order);
    }

    public void placeSellOrder(Order order) {
        Account account = order.getUser().getAccount();
        if (account.getStockQuantity(order.getStock().getSymbol()) < order.getQuantity()) {
            throw new InsufficientStockException("Not enough stock to place sell order.");
        }
        System.out.printf("Placing SELL order %s for %d shares of %s.%n", order.getOrderId(), order.getQuantity(), order.getStock().getSymbol());
        stockExchange.placeSellOrder(order);
    }

    public void cancelOrder(Order order) {
        stockExchange.cancelOrder(order);
    }

    public List<Trade> getTradeHistory() {
        return stockExchange.getTradeHistory();
    }
}


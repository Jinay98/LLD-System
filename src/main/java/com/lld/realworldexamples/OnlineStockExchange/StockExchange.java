package com.lld.realworldexamples.OnlineStockExchange;

import com.lld.realworldexamples.OnlineStockExchange.enums.OrderStatus;
import com.lld.realworldexamples.OnlineStockExchange.enums.OrderType;
import com.lld.realworldexamples.OnlineStockExchange.models.Order;
import com.lld.realworldexamples.OnlineStockExchange.models.OrderBook;
import com.lld.realworldexamples.OnlineStockExchange.models.Stock;
import com.lld.realworldexamples.OnlineStockExchange.models.Trade;
import com.lld.realworldexamples.OnlineStockExchange.models.User;
import com.lld.realworldexamples.OnlineStockExchange.states.FailedState;
import com.lld.realworldexamples.OnlineStockExchange.states.FilledState;
import com.lld.realworldexamples.OnlineStockExchange.states.PartiallyFilledState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StockExchange {
    private static volatile StockExchange instance;
    private final Map<String, OrderBook> orderBooks;
    private final List<Trade> tradeHistory;

    private StockExchange() {
        this.orderBooks = new ConcurrentHashMap<>();
        this.tradeHistory = new CopyOnWriteArrayList<>();
    }

    public static StockExchange getInstance() {
        if (instance == null) {
            synchronized (StockExchange.class) {
                if (instance == null) {
                    instance = new StockExchange();
                }
            }
        }
        return instance;
    }

    public void placeBuyOrder(Order order) {
        synchronized (this) { // Add to the book and match as one atomic step
            getOrderBook(order.getStock().getSymbol()).addBuyOrder(order);
            matchOrders(order.getStock());
        }
    }

    public void placeSellOrder(Order order) {
        synchronized (this) {
            getOrderBook(order.getStock().getSymbol()).addSellOrder(order);
            matchOrders(order.getStock());
        }
    }

    public void cancelOrder(Order order) {
        synchronized (this) {
            order.cancel();
            if (order.getStatus() == OrderStatus.CANCELLED) {
                getOrderBook(order.getStock().getSymbol()).removeOrder(order);
            }
        }
    }

    public List<Trade> getTradeHistory() {
        return List.copyOf(tradeHistory);
    }

    private OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, OrderBook::new);
    }

    private void matchOrders(Stock stock) {
        synchronized (this) { // Critical section to prevent race conditions during matching
            OrderBook book = orderBooks.get(stock.getSymbol());
            if (book == null) return;

            boolean matchFound;
            do {
                matchFound = false;
                Order bestBuy = book.getBestBuy();
                Order bestSell = book.getBestSell();

                if (bestBuy != null && bestSell != null) {
                    // The resting limit order sets the trade price; a market order takes it.
                    double tradePrice;
                    if (bestSell.getType() == OrderType.LIMIT) {
                        tradePrice = bestSell.getPrice();      // seller's ask sets the price
                    } else if (bestBuy.getType() == OrderType.LIMIT) {
                        tradePrice = bestBuy.getPrice();       // market sell takes the buyer's bid
                    } else {
                        tradePrice = stock.getPrice();         // both market: use the current market price
                    }

                    // Each side's strategy decides whether it can execute at this price.
                    boolean buyerCanTrade = bestBuy.getExecutionStrategy().canExecute(bestBuy, tradePrice);
                    boolean sellerCanTrade = bestSell.getExecutionStrategy().canExecute(bestSell, tradePrice);

                    if (buyerCanTrade && sellerCanTrade) {
                        executeTrade(book, bestBuy, bestSell, tradePrice);
                        matchFound = true;
                    }
                }
            } while (matchFound);
        }
    }

    private void executeTrade(OrderBook book, Order buyOrder, Order sellOrder, double tradePrice) {
        User buyer = buyOrder.getUser();
        User seller = sellOrder.getUser();

        // Trade only the shares both sides still have open.
        int tradeQuantity = Math.min(buyOrder.getRemainingQuantity(), sellOrder.getRemainingQuantity());
        double totalCost = tradeQuantity * tradePrice;

        // Validate both sides before moving any cash or shares, so a failure never leaves a half-applied trade.
        if (buyer.getAccount().getBalance() < totalCost) {
            failOrder(book, buyOrder, "buyer has insufficient funds");
            return;
        }
        if (seller.getAccount().getStockQuantity(sellOrder.getStock().getSymbol()) < tradeQuantity) {
            failOrder(book, sellOrder, "seller has insufficient shares");
            return;
        }

        System.out.printf("--- Executing Trade for %s at $%.2f ---%n", buyOrder.getStock(), tradePrice);

        buyer.getAccount().debit(totalCost);
        buyer.getAccount().addStock(buyOrder.getStock().getSymbol(), tradeQuantity);

        seller.getAccount().credit(totalCost);
        seller.getAccount().removeStock(sellOrder.getStock().getSymbol(), tradeQuantity);

        // Record the executed trade in the history.
        tradeHistory.add(new Trade(buyer, seller, buyOrder.getStock(), tradeQuantity, tradePrice));

        updateOrderStatus(book, buyOrder, tradeQuantity);
        updateOrderStatus(book, sellOrder, tradeQuantity);

        // Update stock's market price to last traded price
        buyOrder.getStock().setPrice(tradePrice);

        System.out.println("--- Trade Complete ---");
    }

    private void updateOrderStatus(OrderBook book, Order order, int quantityTraded) {
        order.addFilledQuantity(quantityTraded);
        if (order.getRemainingQuantity() == 0) {
            order.setStatus(OrderStatus.FILLED);
            order.setState(new FilledState());
            book.removeOrder(order);
        } else {
            // Some shares traded but not all; keep the remainder on the book for future matches.
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
            order.setState(new PartiallyFilledState());
        }
    }

    private void failOrder(OrderBook book, Order order, String reason) {
        order.setStatus(OrderStatus.FAILED);
        order.setState(new FailedState());
        book.removeOrder(order);
        System.out.printf("Order %s failed: %s.%n", order.getOrderId(), reason);
    }
}


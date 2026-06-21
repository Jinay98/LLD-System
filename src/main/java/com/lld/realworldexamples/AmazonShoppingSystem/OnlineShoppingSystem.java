package com.lld.realworldexamples.AmazonShoppingSystem;

import com.lld.realworldexamples.AmazonShoppingSystem.exceptions.OutOfStockException;
import com.lld.realworldexamples.AmazonShoppingSystem.models.Address;
import com.lld.realworldexamples.AmazonShoppingSystem.models.Customer;
import com.lld.realworldexamples.AmazonShoppingSystem.models.Order;
import com.lld.realworldexamples.AmazonShoppingSystem.models.Product;
import com.lld.realworldexamples.AmazonShoppingSystem.models.ShoppingCart;
import com.lld.realworldexamples.AmazonShoppingSystem.services.InventoryService;
import com.lld.realworldexamples.AmazonShoppingSystem.services.OrderService;
import com.lld.realworldexamples.AmazonShoppingSystem.services.PaymentService;
import com.lld.realworldexamples.AmazonShoppingSystem.services.SearchService;
import com.lld.realworldexamples.AmazonShoppingSystem.strategies.PaymentStrategy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineShoppingSystem {
    private static volatile OnlineShoppingSystem instance;

    // Data stores
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    // Services
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderService orderService;
    private final SearchService searchService;

    private OnlineShoppingSystem() {
        this.inventoryService = new InventoryService();
        this.paymentService = new PaymentService();
        this.orderService = new OrderService(inventoryService);
        this.searchService = new SearchService(products.values());
    }

    public static OnlineShoppingSystem getInstance() {
        if (instance == null) {
            synchronized (OnlineShoppingSystem.class) {
                if (instance == null) {
                    instance = new OnlineShoppingSystem();
                }
            }
        }
        return instance;
    }

    // --- Facade Methods for simplified interaction ---
    public void addProduct(Product product, int initialStock) {
        products.put(product.getId(), product);
        inventoryService.addStock(product, initialStock);
    }

    public Customer registerCustomer(String name, String email, String password, Address address) {
        Customer customer = new Customer(name, email, password, address);
        customers.put(customer.getId(), customer);
        return customer;
    }

    public void addToCart(String customerId, Product product, int quantity) {
        Customer customer = customers.get(customerId);
        customer.getAccount().getCart().addItem(product, quantity);
    }

    public ShoppingCart getCustomerCart(String customerId) {
        Customer customer = customers.get(customerId);
        return customer.getAccount().getCart();
    }

    public List<Product> searchProducts(String name) {
        return searchService.searchByName(name);
    }

    public Order placeOrder(String customerId, PaymentStrategy paymentStrategy) {
        Customer customer = customers.get(customerId);
        ShoppingCart cart = customer.getAccount().getCart();
        if (cart.getItems().isEmpty()) {
            System.out.println("Cannot place an order with an empty cart.");
            return null;
        }

        // 1. Reserve inventory first, before charging the customer
        Order order;
        try {
            order = orderService.createOrder(customer, cart);
        } catch (OutOfStockException e) {
            System.err.println("Order placement failed: " + e.getMessage());
            return null;
        }

        // 2. Charge payment now that the stock is reserved
        boolean paymentSuccess = paymentService.processPayment(paymentStrategy, cart.calculateTotal());
        if (!paymentSuccess) {
            // Payment failed, so release the stock we reserved
            inventoryService.restock(order.getItems());
            System.out.println("Payment failed. Please try again.");
            return null;
        }

        // 3. Record the order and clear the cart
        orders.put(order.getId(), order);
        cart.clearCart();
        return order;
    }
}

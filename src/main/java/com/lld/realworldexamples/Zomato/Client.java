package com.lld.realworldexamples.Zomato;

import com.lld.realworldexamples.Zomato.enums.OrderTypes;
import com.lld.realworldexamples.Zomato.managers.CartManager;
import com.lld.realworldexamples.Zomato.managers.RestaurantManager;
import com.lld.realworldexamples.Zomato.models.Cart;
import com.lld.realworldexamples.Zomato.models.FoodItem;
import com.lld.realworldexamples.Zomato.models.Restaurant;
import com.lld.realworldexamples.Zomato.models.User;
import com.lld.realworldexamples.Zomato.services.OrderingService;

public class Client {
    public static void main(String[] args) {
        System.out.println("========== ZOMATO FOOD ORDERING SYSTEM ==========\n");

        // Step 0: Setup - Create test data
        setupTestData();

        // Step 1: Create user
        System.out.println("=== Creating User ===");
        User user = new User("Jinay", "4532123456789012", "9876543210", "Delhi", new CartManager());
        System.out.println("User created: " + user.getName() + " from " + user.getLocation());

        // Step 2: User searches for restaurants by location
        System.out.println("\n=== User Searches for Restaurants ===");
        System.out.println("Searching restaurants in: " + user.getLocation());
        RestaurantManager.getInstance().searchRestaurants(user.getLocation());

        // Step 3: User selects restaurant and views menu
        System.out.println("\n=== User Selects Restaurant and Views Menu ===");
        System.out.println("Selected Restaurant: McDonald's");
        RestaurantManager.getInstance().getMenuItemsForARestaurant(1, "McDonald's");

        // Step 4: Create cart and add items
        System.out.println("\n=== User Adds Items to Cart ===");
        Cart cart = new Cart();
        cart.setRestaurant(RestaurantManager.getInstance().getRestaurants().get(0));

        FoodItem burger = new FoodItem(1, "BURGER", "Burger", 100);
        FoodItem coke = new FoodItem(2, "COKE", "Coke", 50);
        FoodItem fries = new FoodItem(3, "FRIES", "Fries", 40);

        user.getCartManager().addFoodItemToCart(cart, burger, 2);
        System.out.println("✓ Added 2x Burger to cart");

        user.getCartManager().addFoodItemToCart(cart, coke, 1);
        System.out.println("✓ Added 1x Coke to cart");

        user.getCartManager().addFoodItemToCart(cart, fries, 1);
        System.out.println("✓ Added 1x Fries to cart");

        // Step 5: View cart
        System.out.println("\n=== Cart Summary ===");
        user.getCartManager().getCartItems(cart);
        System.out.println("Total Amount: ₹" + user.getCartManager().getTotal(cart));

        // Step 6: Checkout
        System.out.println("\n=== User Proceeds to Checkout ===");
        OrderingService.placeOrder(
            user,
            cart,
            OrderTypes.ORDER_NOW,
            "CREDIT_CARD",
            "4532123456789012",
            "SMS"
        );

        System.out.println("\n========== ORDER PLACED SUCCESSFULLY! ==========");
    }

    private static void setupTestData() {
        System.out.println("=== Setting Up Test Data ===");
        RestaurantManager restaurantManager = RestaurantManager.getInstance();

        // Create restaurant 1
        Restaurant mcDonalds = new Restaurant();
        mcDonalds.setId(1);
        mcDonalds.setName("McDonald's");
        mcDonalds.setLocation("Delhi");
        mcDonalds.getMenu().add(new FoodItem(1, "BURGER", "Burger", 100));
        mcDonalds.getMenu().add(new FoodItem(2, "COKE", "Coke", 50));
        mcDonalds.getMenu().add(new FoodItem(3, "FRIES", "Fries", 40));
        restaurantManager.addRestaurant(mcDonalds);

        // Create restaurant 2
        Restaurant subway = new Restaurant();
        subway.setId(2);
        subway.setName("Subway");
        subway.setLocation("Delhi");
        subway.getMenu().add(new FoodItem(4, "SUB_6", "6-Inch Sub", 120));
        subway.getMenu().add(new FoodItem(5, "SUB_12", "12-Inch Sub", 200));
        restaurantManager.addRestaurant(subway);

        System.out.println("✓ Restaurants added to system");
    }
}

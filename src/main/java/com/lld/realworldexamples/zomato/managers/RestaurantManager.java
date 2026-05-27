package com.lld.realworldexamples.zomato.managers;

import com.lld.realworldexamples.zomato.models.FoodItem;
import com.lld.realworldexamples.zomato.models.Restaurant;

import java.util.ArrayList;
import java.util.List;

public class RestaurantManager {
    private static RestaurantManager restaurantManager;

    private List<Restaurant> restaurants = new ArrayList<>();

    private RestaurantManager() {
    }

    public static RestaurantManager getInstance() {
        if (restaurantManager == null) {
            restaurantManager = new RestaurantManager();
        }
        return restaurantManager;
    }

    public List<Restaurant> getRestaurants() {
        return restaurants;
    }

    public void addRestaurant(Restaurant restaurant) {
        this.restaurants.add(restaurant);
    }

    public List<Restaurant> removeRestaurant(int id, String name) {
        List<Restaurant> updatedRestaurants = new ArrayList<>();
        for (Restaurant r : restaurants) {
            if (id == r.getId() && name.equalsIgnoreCase(r.getName())) {
                continue;
            } else {
                updatedRestaurants.add(r);
            }
        }
        return updatedRestaurants;
    }

    public void searchRestaurants(String location) {
        System.out.println("Search results are as follows:");
        for (Restaurant r : restaurants) {
            if (location.equalsIgnoreCase(r.getLocation())) {
                System.out.println(r.getId() + "--" + r.getName() + "--" + r.getLocation());
                System.out.println("*******************************");
            }
        }
    }

    public void getMenuItemsForARestaurant(int id, String name) {
        System.out.println("The Restaurant Menu is as follows:");
        for (Restaurant r : restaurants) {
            if (id == r.getId() && name.equalsIgnoreCase(r.getName())) {
                for (FoodItem item : r.getMenu()) {
                    System.out.println(item.getId() + "--" + item.getName() + "--" + item.getPrice());
                    System.out.println("*********************************");
                }
                break;
            }
        }
    }
}

package com.lld.realworldexamples.zomato.models;

public class OrderItem {
    private FoodItem foodItem;
    private int qty;
    private double priceAtOrderTime;

    public OrderItem(FoodItem foodItem, int qty, double priceAtOrderTime) {
        this.foodItem = foodItem;
        this.qty = qty;
        this.priceAtOrderTime = priceAtOrderTime;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
    }

    public int getQty() {
        return qty;
    }

    public void setQty(int qty) {
        this.qty = qty;
    }

    public double getPriceAtOrderTime() {
        return priceAtOrderTime;
    }

    public void setPriceAtOrderTime(double priceAtOrderTime) {
        this.priceAtOrderTime = priceAtOrderTime;
    }
}

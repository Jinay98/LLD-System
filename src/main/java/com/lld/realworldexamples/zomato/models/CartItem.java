package com.lld.realworldexamples.zomato.models;

public class CartItem {
    private FoodItem foodItem;
    private int qty;

    public CartItem(FoodItem foodItem, int qty) {
        this.foodItem = foodItem;
        this.qty = qty;
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

    public double getSubTotal(){
        return this.foodItem.getPrice() * this.qty;
    }
}


package com.lld.realworldexamples.zomato.models;

import com.lld.realworldexamples.zomato.managers.CartManager;

public class User {
    private String name;
    private String cardNo;
    private String mobileNo;
    private String location;
    private CartManager cartManager;

    public User(String name, String cardNo, String mobileNo, String location, CartManager cartManager) {
        this.name = name;
        this.cardNo = cardNo;
        this.mobileNo = mobileNo;
        this.location = location;
        this.cartManager = cartManager;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public CartManager getCartManager() {
        return cartManager;
    }

    public void setCartManager(CartManager cartManager) {
        this.cartManager = cartManager;
    }
}

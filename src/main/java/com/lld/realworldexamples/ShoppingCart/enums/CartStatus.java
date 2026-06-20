package com.lld.realworldexamples.ShoppingCart.enums;

public enum CartStatus {
    ACTIVE,       // Cart can be modified
    CHECKED_OUT,  // Purchase complete, no modifications allowed
    ABANDONED     // Cart was abandoned, no modifications allowed
}

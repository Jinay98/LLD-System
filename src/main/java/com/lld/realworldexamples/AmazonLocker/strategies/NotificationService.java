package com.lld.realworldexamples.AmazonLocker.strategies;

public interface NotificationService {
    void notifyCustomer(String orderId, String code, String lockerAddress, long expirationTime);
}

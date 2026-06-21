package com.lld.realworldexamples.AmazonLocker.strategies;

public class ConsoleNotificationService implements NotificationService {
    @Override
    public void notifyCustomer(String orderId, String code, String lockerAddress, long expirationTime) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String expiry = sdf.format(new java.util.Date(expirationTime));

        System.out.println("[NOTIFICATION] Order " + orderId + ": Your package is ready for pickup!");
        System.out.println("  Locker Location: " + lockerAddress);
        System.out.println("  Pickup Code: " + code);
        System.out.println("  Expires: " + expiry);
    }
}

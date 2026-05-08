package com.bcommerce.sharding;

public final class OrderSharding {

    private OrderSharding() {}

    public static int slot(long userId) {
        return Math.floorMod(userId, 2);
    }
}

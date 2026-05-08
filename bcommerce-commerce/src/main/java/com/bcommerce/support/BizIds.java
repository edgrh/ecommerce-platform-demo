package com.bcommerce.support;

import java.util.concurrent.atomic.AtomicLong;

public final class BizIds {

    private static final AtomicLong SEQ = new AtomicLong();

    private BizIds() {}

    public static long next() {
        return (System.currentTimeMillis() << 12) | (SEQ.incrementAndGet() & 0xFFF);
    }
}

package com.swz.rpc.retry.impl;

import com.swz.rpc.retry.RetryPolicy;

import java.util.Random;

/**
 * 随机退避
 *
 * @author 向前走不回头
 * @date 2021/8/28
 */
public class RandomBackOffRetry implements RetryPolicy {
    private final Random random = new Random();
    private final long maxSleepMs;
    private final int maxRetries;

    public RandomBackOffRetry(int maxRetries, long maxSleepMs) {
        this.maxRetries = maxRetries;
        this.maxSleepMs = maxSleepMs;
    }

    @Override
    public boolean allowRetry(int retryCount) {
        if (retryCount < maxRetries) {
            return true;
        }
        return false;
    }

    @Override
    public long getSleepTimeMs(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retries count must greater than 0.");
        }
        return 1000L * Math.max(1, random.nextInt((int) (maxSleepMs / 1000)));
    }
}

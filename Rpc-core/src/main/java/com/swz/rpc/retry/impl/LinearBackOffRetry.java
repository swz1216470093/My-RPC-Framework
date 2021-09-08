package com.swz.rpc.retry.impl;

import com.swz.rpc.retry.RetryPolicy;

/**
 * 线性退避
 *
 * @author 向前走不回头
 * @date 2021/8/28
 */
public class LinearBackOffRetry implements RetryPolicy {
    private final int maxRetries;
    private final long sleepTimeMs;

    public LinearBackOffRetry(int maxRetries, long sleepTimeMs) {
        this.maxRetries = maxRetries;
        this.sleepTimeMs = sleepTimeMs;
    }

    @Override
    public boolean allowRetry(int retryCount) {
        return retryCount < maxRetries;
    }

    @Override
    public long getSleepTimeMs(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("retries count must greater than 0.");
        }
        return sleepTimeMs;
    }
}

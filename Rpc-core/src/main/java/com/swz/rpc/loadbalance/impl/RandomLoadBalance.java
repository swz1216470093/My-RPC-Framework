package com.swz.rpc.loadbalance.impl;

import com.swz.rpc.loadbalance.AbstractLoadBalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    private final Random random = new Random();

    @Override
    public InetSocketAddress doSelect(List<InetSocketAddress> serviceAddress, String serviceName) {
        int index = random.nextInt(serviceAddress.size());
        return serviceAddress.get(index);
    }

    static class Holder {
        private static final RandomLoadBalance INSTANCE = new RandomLoadBalance();
    }

    public static RandomLoadBalance getInstance() {
        return Holder.INSTANCE;
    }
}

package com.swz.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
public class RoundLoadBalance extends AbstractLoadBalance{
    private final AtomicInteger choose = new AtomicInteger();

    @Override
    public InetSocketAddress doSelect(List<InetSocketAddress> serviceAddress) {
        return serviceAddress.get(choose.getAndIncrement() % serviceAddress.size());
    }
    static class Holder{
        private static final RoundLoadBalance INSTANCE = new RoundLoadBalance();
    }
    public static RoundLoadBalance getInstance() {
        return Holder.INSTANCE;
    }
}

package com.swz.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class RandomLoadBalance implements LoadBalance{
    private final Random random = new Random();
    @Override
    public InetSocketAddress selectServiceAddress(List<InetSocketAddress> serviceAddress) {
        int index = random.nextInt(serviceAddress.size());
        return serviceAddress.get(index);
    }
}

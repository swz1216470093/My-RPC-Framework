package com.swz.rpc.loadbalance.impl;

import com.google.common.hash.Hashing;
import com.swz.rpc.loadbalance.AbstractLoadBalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author 向前走不回头
 * @date 2021/9/8
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    @Override
    public InetSocketAddress doSelect(List<InetSocketAddress> serviceAddress, String serviceName) {
        int index = Hashing.consistentHash(serviceName.hashCode(), serviceAddress.size());
        return serviceAddress.get(index);
    }

}

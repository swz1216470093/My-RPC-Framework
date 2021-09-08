package com.swz.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
public abstract class AbstractLoadBalance implements LoadBalance{
    @Override
    public InetSocketAddress selectServiceAddress(List<InetSocketAddress> serviceAddress, String serviceName) {
        if (serviceAddress == null || serviceAddress.size() == 0) {
            return null;
        }
        if (serviceAddress.size() == 1) {
            return serviceAddress.get(0);
        }
        return doSelect(serviceAddress, serviceName);
    }

    public abstract InetSocketAddress doSelect(List<InetSocketAddress> serviceAddress, String serviceName);
}

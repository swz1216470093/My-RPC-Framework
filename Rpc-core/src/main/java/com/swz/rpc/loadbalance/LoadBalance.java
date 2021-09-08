package com.swz.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public interface LoadBalance {

    /**
     * 选择一个服务地址
     *
     * @param serviceAddress 服务地址列表
     * @param serviceName    服务名
     * @return 服务地址
     */
    InetSocketAddress selectServiceAddress(List<InetSocketAddress> serviceAddress, String serviceName);
}

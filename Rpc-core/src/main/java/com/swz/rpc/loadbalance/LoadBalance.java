package com.swz.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public interface LoadBalance {

    InetSocketAddress selectServiceAddress(List<InetSocketAddress> serviceAddress);
}

package com.swz.rpc.registry;

import java.net.InetSocketAddress;

/**
 * 注册中心
 * @author 向前走不回头
 * @date 2021/7/23
 */
public interface Registry {

    /**
     * 注册服务
     * @param service
     * @param serviceName
     * @param address
     */
    void registerService(Object service, String serviceName, InetSocketAddress address);

    /**
     * 查找服务地址
     * @param serviceName
     * @return
     */
    InetSocketAddress lookupServiceAddress(String serviceName);

    /**
     * 得到服务实例
     * @param serviceName
     * @return
     */
    Object getService(String serviceName);
}

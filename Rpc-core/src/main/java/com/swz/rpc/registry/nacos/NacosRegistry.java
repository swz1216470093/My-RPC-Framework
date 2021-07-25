package com.swz.rpc.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.swz.rpc.exception.RpcException;
import com.swz.rpc.loadbalance.LoadBalance;
import com.swz.rpc.loadbalance.RandomLoadBalance;
import com.swz.rpc.registry.Registry;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
@Slf4j
public class NacosRegistry implements Registry {
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredService;
    private static final String SERVER_ADDR = "127.0.0.1:8849";
    private static final NamingService namingService;
    private final LoadBalance loadBalance;
    static {
        namingService = getNamingService();
    }
    private static NamingService getNamingService(){
        try {
            Properties properties = new Properties();
            properties.setProperty("serverAddr",SERVER_ADDR);
            return NamingFactory.createNamingService(properties);
        } catch (NacosException e) {
            log.error("连接到Nacos时有错误发生: ", e);
            throw new RpcException("连接到nacos时错误");
        }
    }

    private NacosRegistry() {
        serviceMap = new ConcurrentHashMap<>();
        registeredService = ConcurrentHashMap.newKeySet();
        loadBalance = new RandomLoadBalance();
    }

    @Override
    public void registerService(Object service, String serviceName, InetSocketAddress address) {
        serviceMap.put(serviceName,service);
        registeredService.add(serviceName);
        Instance instance = new Instance();
        instance.setPort(address.getPort());
        instance.setIp(address.getHostName());
        instance.setServiceName(serviceName);
        try {
            namingService.registerInstance(serviceName,instance);
        } catch (NacosException e) {
            log.debug("注册服务时出错" + e.getMessage());
        }
    }

    @Override
    public InetSocketAddress lookupServiceAddress(String serviceName) {
        try {
            List<InetSocketAddress> addresses = getServiceAddress(serviceName);
            return loadBalance.selectServiceAddress(addresses);
        } catch (NacosException e) {
           throw new RpcException("服务发现时出错");
        }
    }

    private List<InetSocketAddress> getServiceAddress(String serviceName) throws NacosException {
        List<Instance> instances = namingService.getAllInstances(serviceName);
        List<InetSocketAddress> addresses =  new ArrayList<>();
        for (Instance instance : instances) {
            addresses.add(new InetSocketAddress(instance.getIp(), instance.getPort()));
        }
        return addresses;
    }

    @Override
    public Object getService(String serviceName){
        Object service = serviceMap.get(serviceName);
        if (null == service) {
            throw new RpcException("服务未找到");
        }
        return service;
    }

    static class Holder{
        private static final NacosRegistry INSTANCE = new NacosRegistry();
    }

    public static NacosRegistry getInstance() {
        return Holder.INSTANCE;
    }
}

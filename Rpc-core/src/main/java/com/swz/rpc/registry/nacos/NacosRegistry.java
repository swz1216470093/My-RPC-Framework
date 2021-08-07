package com.swz.rpc.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
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
    private final Set<String> registeredService;
    private final Map<String ,List<Instance>> serviceInstanceCache;
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
        registeredService = ConcurrentHashMap.newKeySet();
        loadBalance = RandomLoadBalance.getInstance();
        serviceInstanceCache = new ConcurrentHashMap<>();
    }

    @Override
    public void registerService(String serviceName, InetSocketAddress address) {
        registeredService.add(serviceName);
        Instance instance = new Instance();
        instance.setPort(address.getPort());
        instance.setIp(address.getHostName());
        instance.setServiceName(serviceName);
        try {
            namingService.registerInstance(serviceName,instance);
            subscribe(serviceName);
        } catch (NacosException e) {
            log.debug("注册服务时出错" + e.getMessage());
        }
    }

    public void subscribe(String serviceName){
        try {
            namingService.subscribe(serviceName, event -> {
                if (event instanceof NamingEvent){
                    final List<Instance> instances = ((NamingEvent) event).getInstances();
                    if (instances == null || instances.isEmpty()){
                        serviceInstanceCache.remove(serviceName);
                    }else{
                        serviceInstanceCache.put(serviceName,instances);
                    }
                }
            });
        } catch (NacosException e) {
            log.debug("订阅服务时出错" + e.getMessage());
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
        final List<Instance> instances;
        if (serviceInstanceCache.get(serviceName) != null){
            instances = serviceInstanceCache.get(serviceName);
        }else {
            instances = namingService.getAllInstances(serviceName);
        }
        List<InetSocketAddress> addresses =  new ArrayList<>();
        for (Instance instance : instances) {
            addresses.add(new InetSocketAddress(instance.getIp(), instance.getPort()));
        }
        return addresses;
    }


    static class Holder{
        private static final NacosRegistry INSTANCE = new NacosRegistry();
    }

    public static NacosRegistry getInstance() {
        return Holder.INSTANCE;
    }
}

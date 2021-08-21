package com.swz.rpc;

import com.swz.rpc.annotation.RpcService;
import com.swz.rpc.config.RpcConfig;
import com.swz.rpc.container.BeanContainer;
import com.swz.rpc.container.DependencyInjector;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.registry.nacos.NacosRegistry;
import com.swz.rpc.transport.RpcServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author 向前走不回头
 * @date 2021/8/7
 */
public class RpcServerStarter {
    private boolean isStart;
    private final Registry registry;
    private final RpcServer rpcServer;

    public RpcServerStarter(RpcServer rpcServer) {
        registry = ServiceLoader.load(Registry.class).iterator().next();
        this.rpcServer = rpcServer;
    }


    public void start(String basePackage) {
        if (isStart) {
            return;
        }
        BeanContainer beanContainer = BeanContainer.getInstance();
//        加载bean
        beanContainer.loadBeans(basePackage);
//        找到容器中所有标记了RpcService注解的类
        Set<Class<?>> rpcServiceClass = beanContainer.getClassesAnnotatedWith(RpcService.class);
//        将他们注册到注册中心
        for (Class<?> serviceClass : rpcServiceClass) {
            try {
                registry.registerService(serviceClass.getInterfaces()[0].getName(), new InetSocketAddress(InetAddress.getLocalHost(), RpcConfig.PORT));
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        rpcServer.start();
        isStart = true;
    }
}

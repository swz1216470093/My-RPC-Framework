package com.swz.rpc;

import com.swz.rpc.annotation.RpcService;
import com.swz.rpc.container.BeanContainer;
import com.swz.rpc.container.DependencyInjector;
import com.swz.rpc.proxy.ServiceProxy;
import com.swz.rpc.proxy.jdk.JdkServiceProxy;
import com.swz.rpc.transport.RpcTransport;

import java.util.Set;

/**
 * @author 向前走不回头
 * @date 2021/8/7
 */
public class RpcClientStarter {
    private boolean isStart;
    private BeanContainer beanContainer;
    public RpcClientStarter(){
        beanContainer = BeanContainer.getInstance();
    }
    public void start(String basePackage){
        if (isStart){
            return;
        }
//        加载bean
        beanContainer.loadBeans(basePackage);
        new DependencyInjector().doIoc();
        isStart = true;
    }
}

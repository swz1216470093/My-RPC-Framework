package com.swz.rpc;


import com.swz.rpc.container.BeanContainer;
import com.swz.rpc.container.DependencyInjector;


/**
 * @author 向前走不回头
 * @date 2021/8/7
 */
public class RpcClientStarter {
    private boolean isStart;
    private final BeanContainer beanContainer;

    public RpcClientStarter() {
        beanContainer = BeanContainer.getInstance();
    }

    public void start(String basePackage) {
        if (isStart) {
            return;
        }
//        加载bean
        beanContainer.loadBeans(basePackage);
        //依赖注入
        new DependencyInjector().doIoc();
        isStart = true;
    }
}

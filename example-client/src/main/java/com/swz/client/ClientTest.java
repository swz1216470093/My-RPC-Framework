package com.swz.client;

import com.swz.rpc.RpcClientStarter;
import com.swz.rpc.container.BeanContainer;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class ClientTest {
    public static void main(String[] args) {
        //指定扫描包
        new RpcClientStarter().start("com.swz.client");
        BeanContainer container = BeanContainer.getInstance();
        //从容器中获取bean
        HelloController helloController = (HelloController)container.getBean(HelloController.class);
        helloController.test("java");
        helloController.test("netty");
        helloController.test("rpc");
    }
}

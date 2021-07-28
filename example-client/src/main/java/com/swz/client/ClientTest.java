package com.swz.client;

import com.swz.api.HelloService;
import com.swz.rpc.proxy.cglib.CglibServiceProxy;
import com.swz.rpc.proxy.jdk.JdkServiceProxy;
import com.swz.rpc.transport.socket.SocketClient;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class ClientTest {
    public static void main(String[] args) {
        final HelloService helloService1 = (HelloService) new CglibServiceProxy(new SocketClient()).getProxy(HelloService.class);
        System.out.println(helloService1.say("rpc"));
        final HelloService helloService2 = (HelloService) new JdkServiceProxy(new SocketClient()).getProxy(HelloService.class);
        System.out.println(helloService2.say("java"));

    }
}

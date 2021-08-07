package com.swz.client;

import com.swz.api.HelloService;
import com.swz.rpc.annotation.RpcComponent;
import com.swz.rpc.annotation.RpcAutowired;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
@RpcComponent
public class HelloController {
    @RpcAutowired
    HelloService helloService;

    public void test(String name) {
        System.out.println(helloService.say(name));
    }
}

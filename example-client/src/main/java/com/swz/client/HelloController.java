package com.swz.client;

import com.swz.api.HelloService;
import com.swz.rpc.annotation.RpcAutowired;
import com.swz.rpc.annotation.RpcComponent;

import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
@RpcComponent
public class HelloController {
    @RpcAutowired(timeout = 5000, timeunit = TimeUnit.MILLISECONDS)
    HelloService helloService;

    public void test(String name) {
        System.out.println(helloService.say(name));
    }
}

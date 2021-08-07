package com.swz.client;

import com.swz.api.HelloService;
import com.swz.rpc.annotation.RpcComponent;
import com.swz.rpc.annotation.RpcAutowired;

/**
 * todo:实现依赖注入 将helloService的代理对象注入到helloService成员变量中
 * @author 向前走不回头
 * @date 2021/7/24
 */
@RpcComponent
public class HelloController {
    @RpcAutowired
    HelloService helloService;

    public void test() {
        System.out.println(helloService.say("rpc"));
    }
}

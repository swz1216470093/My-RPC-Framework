package com.swz.server.service;

import com.swz.api.HelloService;
import com.swz.rpc.annotation.RpcService;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
@RpcService
public class HelloServiceImpl implements HelloService {
    @Override
    public String say(String name) {
//        int i = 1 / 0;
        return "hello " + name;
    }
}

package com.swz.rpc.proxy.jdk;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.proxy.ServiceProxy;
import com.swz.rpc.transport.RpcTransport;
import com.swz.rpc.transport.netty.client.NettyClient;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
@Slf4j
public class JdkServiceProxy implements ServiceProxy {
    private final RpcTransport rpcTransport;

    public JdkServiceProxy() {
        rpcTransport = new NettyClient();
    }

    public JdkServiceProxy(RpcTransport rpcTransport) {
        this.rpcTransport = rpcTransport;
    }

    @Override
    public <T> Object getProxy(Class<T> clazz) {
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, (proxy, method, args) -> {
            String requestId = UUID.randomUUID().toString();
            RequestMessage requestMessage = new RequestMessage(requestId,
                    clazz.getName(),
                    method.getName(),
                    method.getParameterTypes(),
                    args);
            Promise<Object> promise = (Promise<Object>) rpcTransport.sendRpcRequest(requestMessage);
            try {
                promise.await();
            } catch (InterruptedException e) {
                throw new RpcException("等待远程调用结果时异常 " + e.getMessage());
            }
            if (promise.isSuccess()) {
//                调用正常
                return promise.getNow();
            } else {
//                调用异常
                throw new RpcException(promise.cause());
            }
        });
    }
}

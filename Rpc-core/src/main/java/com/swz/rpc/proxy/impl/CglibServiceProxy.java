package com.swz.rpc.proxy.impl;

import com.swz.rpc.config.RpcConfig;
import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.proxy.ServiceProxy;
import com.swz.rpc.retry.RetryPolicy;
import com.swz.rpc.transport.RpcTransport;
import com.swz.rpc.transport.netty.client.NettyClient;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
@Slf4j
public class CglibServiceProxy implements ServiceProxy {
    private final RpcTransport rpcTransport;

    public CglibServiceProxy(RpcTransport rpcTransport, RetryPolicy retryPolicy) {
        this.rpcTransport = rpcTransport;
    }

    public CglibServiceProxy() {
        rpcTransport = new NettyClient();
    }

    @Override
    public <T> Object getProxy(Class<T> clazz) {
        return Enhancer.create(clazz, (MethodInterceptor) (o, method, objects, methodProxy) -> {
            String requestId = UUID.randomUUID().toString();
            RequestMessage requestMessage = new RequestMessage(requestId,
                    clazz.getName(),
                    method.getName(),
                    method.getParameterTypes(),
                    objects);
            CompletableFuture<Object> future = (CompletableFuture<Object>) rpcTransport.sendRpcRequest(requestMessage);
            try {
                return future.get(RpcConfig.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.debug("远程调用超时");
                throw new RpcException("远程调用超时", e);
            }
        });
    }

    @Override
    public <T> Object getProxyWithTimeout(Class<T> clazz, long timeout, TimeUnit timeUnit) {
        return Enhancer.create(clazz, (MethodInterceptor) (o, method, objects, methodProxy) -> {
            String requestId = UUID.randomUUID().toString();
            RequestMessage requestMessage = new RequestMessage(requestId,
                    clazz.getName(),
                    method.getName(),
                    method.getParameterTypes(),
                    objects);
            CompletableFuture<Object> future = (CompletableFuture<Object>) rpcTransport.sendRpcRequest(requestMessage);
            try {
                return future.get(timeout, timeUnit);
            } catch (TimeoutException e) {
                log.debug("远程调用超时");
                throw new RpcException("远程调用超时", e);
            }
        });
    }
}

package com.swz.rpc.proxy.jdk;

import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.proxy.ServiceProxy;
import com.swz.rpc.transport.RpcTransport;
import com.swz.rpc.transport.netty.client.NettyClient;
import io.netty.util.concurrent.Promise;

import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class JdkServiceProxy implements ServiceProxy {
    private final RpcTransport rpcTransport;

    public JdkServiceProxy() {
        rpcTransport = new NettyClient();
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
            return rpcTransport.sendRpcRequest(requestMessage);
        });
    }
}

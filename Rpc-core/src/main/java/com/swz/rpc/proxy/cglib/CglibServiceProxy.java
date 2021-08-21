package com.swz.rpc.proxy.cglib;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.proxy.ServiceProxy;
import com.swz.rpc.transport.RpcTransport;
import com.swz.rpc.transport.netty.client.NettyClient;
import io.netty.util.concurrent.Promise;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.util.UUID;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
public class CglibServiceProxy implements ServiceProxy {
    private final RpcTransport rpcTransport;
    public CglibServiceProxy(RpcTransport rpcTransport){
        this.rpcTransport = rpcTransport;
    }

    public CglibServiceProxy(){
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

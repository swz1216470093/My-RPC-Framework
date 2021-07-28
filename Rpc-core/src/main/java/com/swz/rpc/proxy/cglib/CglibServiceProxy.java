package com.swz.rpc.proxy.cglib;

import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.proxy.ServiceProxy;
import com.swz.rpc.transport.RpcTransport;
import com.swz.rpc.transport.netty.client.NettyClient;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.tools.ant.taskdefs.compilers.Gcj;

import java.lang.reflect.Method;
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
            return rpcTransport.sendRpcRequest(requestMessage);
        });
    }
}

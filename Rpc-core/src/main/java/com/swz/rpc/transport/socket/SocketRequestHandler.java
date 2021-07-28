package com.swz.rpc.transport.socket;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.pojo.ResponseMessage;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.registry.nacos.NacosRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
@Slf4j
public class SocketRequestHandler implements Runnable{
    private final Socket socket;
    private final Registry registry;
    public SocketRequestHandler(Socket socket) {
        this.socket = socket;
        this.registry = NacosRegistry.getInstance();
    }
    @Override
    public void run() {
        try ( ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
              ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())){
            RequestMessage requestMessage = (RequestMessage) ois.readObject();
            //从注册中心找到服务对象 反射调用请求的方法 用响应消息包装返回值
            Object service = registry.getService(requestMessage.getInterfaceName());
            ResponseMessage responseMessage = new ResponseMessage();
            try {
                Method method = service.getClass().getMethod(requestMessage.getMethodName(), requestMessage.getParameterTypes());
                Object returnValue = method.invoke(service, requestMessage.getParameterValue());
                responseMessage.setReturnValue(returnValue);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                log.debug("远程调用出错");
                responseMessage.setExceptionValue(new RpcException("远程调用出错"+e.getMessage()));
            }
            oos.writeObject(responseMessage);
            oos.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}

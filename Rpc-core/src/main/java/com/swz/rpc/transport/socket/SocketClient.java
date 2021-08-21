package com.swz.rpc.transport.socket;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.pojo.ResponseMessage;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.registry.nacos.NacosRegistry;
import com.swz.rpc.transport.RpcTransport;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ServiceLoader;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
public class SocketClient implements RpcTransport {
    private final Registry registry;
    public SocketClient(){
        registry = ServiceLoader.load(Registry.class).iterator().next();
    }
    @Override
    public Object sendRpcRequest(RequestMessage requestMessage) {
        //        从注册中心找到服务地址
        InetSocketAddress address = registry.lookupServiceAddress(requestMessage.getInterfaceName());
        try (Socket socket = new Socket()){
            socket.connect(address);
            final OutputStream outputStream = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(requestMessage);
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            ResponseMessage responseMessage = (ResponseMessage) objectInputStream.readObject();
            if (responseMessage.getExceptionValue() == null) {
//               正常返回
                return responseMessage.getReturnValue();
            }else {
                //                调用异常
                throw new RpcException(responseMessage.getExceptionValue());
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RpcException("调用服务失败:", e);
        }
    }

}

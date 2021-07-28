package com.swz.rpc.transport.socket;

import com.swz.rpc.annotation.RpcService;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.registry.nacos.NacosRegistry;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.io.IOException;
import java.net.*;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
@Slf4j
public class SocketServer {
    private static final int PORT = 9090;
    private final Registry registry;
    private final ExecutorService executorService;
    public SocketServer(){
        executorService = new ThreadPoolExecutor(8,1024,5, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.DiscardPolicy());
        registry = NacosRegistry.getInstance();
    }

    public void start(){
        try (ServerSocket server = new ServerSocket()) {
            String host = InetAddress.getLocalHost().getHostAddress();
            server.bind(new InetSocketAddress(host, PORT));
            Socket socket;
            while ((socket = server.accept()) != null){
                log.info("client connected [{}]", socket.getInetAddress());
                executorService.execute(new SocketRequestHandler(socket));
            }
        } catch (IOException e) {
            log.error("occur IOException:", e);
        }
    }

    public void scanPackage(String basePackage){
        Reflections reflections = new Reflections(basePackage);
        Set<Class<?>> rpcServiceClass = reflections.getTypesAnnotatedWith(RpcService.class);
//        找到所有标记了RpcService注解的类
//        将他们注册到注册中心
        for (Class<?> serviceClass : rpcServiceClass) {
            try {
                Object service = serviceClass.newInstance();
                registry.registerService(service, serviceClass.getInterfaces()[0].getName(),new InetSocketAddress(InetAddress.getLocalHost(),PORT));
            } catch (InstantiationException | IllegalAccessException | UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
}

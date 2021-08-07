package com.swz.rpc.transport.socket;

import com.swz.rpc.annotation.RpcService;
import com.swz.rpc.config.RpcConfig;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.registry.nacos.NacosRegistry;
import com.swz.rpc.transport.RpcServer;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/7/28
 */
@Slf4j
public class SocketServer implements RpcServer {
    private final ExecutorService executorService;
    public SocketServer(){
        executorService = new ThreadPoolExecutor(8,1024,5, TimeUnit.SECONDS,new ArrayBlockingQueue<>(100),new ThreadPoolExecutor.DiscardPolicy());
    }

    public void start(){
        try (ServerSocket server = new ServerSocket()) {
            String host = InetAddress.getLocalHost().getHostAddress();
            server.bind(new InetSocketAddress(host, RpcConfig.PORT));
            Socket socket;
            while ((socket = server.accept()) != null){
                log.info("client connected [{}]", socket.getInetAddress());
                executorService.execute(new SocketRequestHandler(socket));
            }
        } catch (IOException e) {
            log.error("occur IOException:", e);
        }
    }

}

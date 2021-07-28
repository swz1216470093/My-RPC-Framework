package com.swz.server;

import com.swz.rpc.transport.netty.server.NettyServer;
import com.swz.rpc.transport.socket.SocketServer;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class ServerTest {
    public static void main(String[] args) {
//        final NettyServer nettyServer = new NettyServer();
//        扫描basepackage包下使用了RPCService注解的类 将它们注册到注册中心
//        nettyServer.scanPackage("com.swz.server");
//        nettyServer.start();
        final SocketServer socketServer = new SocketServer();
        socketServer.scanPackage("com.swz.server");
        socketServer.start();
    }
}

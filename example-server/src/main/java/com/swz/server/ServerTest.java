package com.swz.server;

import com.swz.rpc.RpcServerStarter;
import com.swz.rpc.transport.netty.server.NettyServer;
import com.swz.rpc.transport.socket.SocketServer;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class ServerTest {
    public static void main(String[] args) {
        new RpcServerStarter(new NettyServer()).start("com.swz.server");
//        final SocketServer socketServer = new SocketServer();
//        socketServer.scanPackage("com.swz.server");
//        socketServer.start();
    }
}

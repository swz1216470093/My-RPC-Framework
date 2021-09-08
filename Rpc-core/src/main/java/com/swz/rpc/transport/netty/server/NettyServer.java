package com.swz.rpc.transport.netty.server;

import com.swz.rpc.config.RpcConfig;
import com.swz.rpc.transport.RpcServer;
import com.swz.rpc.transport.netty.handler.ServerHandler;
import com.swz.rpc.transport.netty.protocol.MessageCodec;
import com.swz.rpc.transport.netty.protocol.ProtocolDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
@Slf4j
public class NettyServer implements RpcServer {
    @Override
    public void start(){
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        DefaultEventExecutorGroup executors = new DefaultEventExecutorGroup(4);
        serverBootstrap.group(boss, worker)
                //启用nagle算法，尽可能的发送大数据包
                .childOption(ChannelOption.TCP_NODELAY, true)
                //全连接队列长度
                .option(ChannelOption.SO_BACKLOG,128)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
//                                空闲检测 解决连接假死问题
//                                当读空闲时即30秒内没有收到客户端请求就关闭连接
                                .addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
//                               帧解码器 解决半包粘包问题
                                .addLast(new ProtocolDecoder())
//                                消息编解码器
                                .addLast(new MessageCodec())
                                .addLast(executors,new ServerHandler());
                    }
                });
        try {
            serverBootstrap.bind(RpcConfig.PORT)
                    .sync()
                    .channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            log.debug("服务端启动失败");
            e.printStackTrace();
        }finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            executors.shutdownGracefully();
        }
    }

}

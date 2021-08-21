package com.swz.rpc.transport.netty.client;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.PingMessage;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.registry.nacos.NacosRegistry;
import com.swz.rpc.transport.RpcTransport;
import com.swz.rpc.transport.netty.handler.ClientHandler;
import com.swz.rpc.transport.netty.protocol.MessageCodec;
import com.swz.rpc.transport.netty.protocol.ProtocolDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
@Slf4j
public class NettyClient implements RpcTransport {

    private final Bootstrap bootstrap;
    private final Registry registry;
    private final ChannelProvider channelProvider;

    public NettyClient() {
        registry = ServiceLoader.load(Registry.class).iterator().next();
        channelProvider = ChannelProvider.getInstance();
        bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
//                连接超时时间5毫秒
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
//                              // 用来判断是不是 读空闲时间过长，或 写空闲时间过长
//                              20秒内如果没有向服务器写数据，会触发一个 IdleState#WRITER_IDLE 事件
                                .addLast(new IdleStateHandler(0, 20, 0, TimeUnit.SECONDS))
//                                帧解码器
                                .addLast(new ProtocolDecoder())
//                                消息编解码器
                                .addLast(new MessageCodec())
//                                客户端处理器
                                .addLast(new ClientHandler());
                    }
                });
    }

    private Channel doConnect(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("与服务端{}建立连接成功", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    public Channel getChannel(InetSocketAddress address) {
        Channel channel = channelProvider.get(address);
//        先去缓存中找
        if (channel != null) {
            return channel;
        } else {
//            缓存中找不到 建立连接 放入缓存
            try {
                channel = doConnect(address);
                channelProvider.put(address, channel);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                log.error("获取Channel时出错");
            }
        }
        return channel;
    }

    @Override
    public Object sendRpcRequest(RequestMessage requestMessage) {
//        从注册中心找到服务地址
        InetSocketAddress address = registry.lookupServiceAddress(requestMessage.getInterfaceName());
//        找到channel
        Channel channel = getChannel(address);
        log.debug("channel{}", channel.toString());
        if (channel.isActive()) {
            Promise<Object> promise = new DefaultPromise<>(channel.eventLoop());
            UnProcessRequest.getInstance().put(requestMessage.getRequestId(), promise);
            channel.writeAndFlush(requestMessage).addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.debug("client send message: [{}]", requestMessage);
                } else {
                    channelFuture.channel().close();
                    promise.setFailure(channelFuture.cause());
                    log.error("Send failed:", channelFuture.cause());
                }
            });
           return promise;
        } else {
            throw new IllegalStateException();
        }
    }
}

package com.swz.rpc.transport.netty.client;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.retry.RetryPolicy;
import com.swz.rpc.retry.impl.ExponentialBackOffRetry;
import com.swz.rpc.transport.RpcTransport;
import com.swz.rpc.transport.netty.handler.ClientHandler;
import com.swz.rpc.transport.netty.handler.ReconnectHandler;
import com.swz.rpc.transport.netty.protocol.MessageCodec;
import com.swz.rpc.transport.netty.protocol.ProtocolDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
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
    private final RetryPolicy retryPolicy;
    private final NioEventLoopGroup group;

    public NettyClient() {
        registry = ServiceLoader.load(Registry.class).iterator().next();
        channelProvider = ChannelProvider.getInstance();
        retryPolicy = new ExponentialBackOffRetry(1000, 3, 60 * 1000);
        ReconnectHandler reconnectHandler = new ReconnectHandler(this);
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
//                连接超时时间5毫秒
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(reconnectHandler)
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

    public CompletableFuture<Channel> doConnect(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.debug("与服务端{}建立连接成功", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
//                失败 触发inactive再次重试
//                这里由于重试产生的新的Channel无法得到要连接的地址 所以使用AttributeKey将地址作为附件传递过去
                if (!future.channel().hasAttr(AttributeKey.valueOf("address"))) {
                    Attribute<InetSocketAddress> address = future.channel().attr(AttributeKey.valueOf("address"));
                    address.set(inetSocketAddress);
                }
                future.channel().pipeline().fireChannelInactive();
            }
        });
        return completableFuture;
    }

    public Channel getChannel(InetSocketAddress address) {
        Channel channel = channelProvider.get(address);
//        先去缓存中找
        if (channel != null) {
            return channel;
        } else {
//            缓存中找不到 建立连接 放入缓存
            try {
                CompletableFuture<Channel> future = doConnect(address);
                channel = future.get();
                channelProvider.put(address, channel);
            } catch (ExecutionException | InterruptedException e) {
                log.error("获取Channel时出错{}", e.getMessage());
            }
        }
        return channel;
    }

    @Override
    public Object sendRpcRequest(RequestMessage requestMessage) {
//        从注册中心找到服务地址
        InetSocketAddress address = registry.lookupServiceAddress(requestMessage.getInterfaceName());
        if (address == null) {
            throw new RpcException("未找到服务地址");
        }
        //        找到channel
        Channel channel = getChannel(address);
        log.debug("channel{}", channel.toString());
        if (channel.isActive()) {
            CompletableFuture<Object> future = new CompletableFuture<>();
            UnProcessRequest.getInstance().put(requestMessage.getRequestId(), future);
            channel.writeAndFlush(requestMessage).addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    log.debug("client send message: [{}]", requestMessage);
                } else {
                    channelFuture.channel().close();
                    future.completeExceptionally(channelFuture.cause());
                    log.error("Send failed:", channelFuture.cause());
                }
            });
            return future;
        } else {
            throw new IllegalStateException();
        }
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void close() {
        group.shutdownGracefully();
    }
}

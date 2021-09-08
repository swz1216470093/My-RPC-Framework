package com.swz.rpc.transport.netty.handler;

import com.swz.rpc.retry.RetryPolicy;
import com.swz.rpc.transport.netty.client.NettyClient;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.HashedWheelTimer;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/8/28
 */
@Slf4j
@ChannelHandler.Sharable
public class ReconnectHandler extends ChannelInboundHandlerAdapter {

    private int retries = 0;
    private RetryPolicy retryPolicy;
    private HashedWheelTimer timer;
    private final NettyClient nettyClient;

    public ReconnectHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        timer = new HashedWheelTimer();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Successfully established a connection to the server.");
        retries = 0;
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (retries == 0) {
            log.error("Lost the TCP connection with the server.");
            ctx.close();
        }
        boolean allowRetry = getRetryPolicy().allowRetry(retries);
        if (allowRetry) {
            long sleepTimeMs = getRetryPolicy().getSleepTimeMs(retries);
            ++retries;
            log.debug("Try to reconnect to the server after {}ms. Retry count: {}.", sleepTimeMs, retries);
            timer.newTimeout((timeout) -> {
                log.debug("Reconnecting ...");
                InetSocketAddress address;
                if (ctx.channel().remoteAddress() != null) {
                    address = (InetSocketAddress) ctx.channel().remoteAddress();
                } else {
                    Attribute<InetSocketAddress> attr = ctx.channel().attr(AttributeKey.valueOf("address"));
                    address = attr.get();
                }
                try {
                    nettyClient.doConnect(address);
                } catch (ExecutionException | InterruptedException e) {
                    log.debug("建立连接时失败: {}", e.getMessage());
                }
            }, sleepTimeMs, TimeUnit.MILLISECONDS);
        } else {
            nettyClient.close();
            timer.stop();
        }
        ctx.fireChannelInactive();
    }

    private RetryPolicy getRetryPolicy() {
        if (retryPolicy == null) {
            retryPolicy = nettyClient.getRetryPolicy();
        }
        return retryPolicy;
    }
}

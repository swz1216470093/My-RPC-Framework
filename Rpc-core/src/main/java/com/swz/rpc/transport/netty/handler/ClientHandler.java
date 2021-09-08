package com.swz.rpc.transport.netty.handler;

import com.swz.rpc.pojo.Message;
import com.swz.rpc.pojo.PingMessage;
import com.swz.rpc.pojo.ResponseMessage;
import com.swz.rpc.transport.netty.client.UnProcessRequest;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<Message> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        if (msg.getMessageType() == Message.RPC_MESSAGE_TYPE_RESPONSE) {
            ResponseMessage responseMessage = (ResponseMessage) msg;
            String requestId = responseMessage.getRequestId();
//            通过requestID得到CompletableFuture 将结果放入future容器中
            CompletableFuture<Object> future = UnProcessRequest.getInstance().remove(requestId);
            if (responseMessage.getExceptionValue() == null) {
//               成功 将结果放入CompletableFuture中
                future.complete(responseMessage.getReturnValue());
            } else {
//                失败 将异常对象放入CompletableFuture中
                future.completeExceptionally(responseMessage.getExceptionValue());
            }
        }else if (msg.getMessageType() == Message.PONG_MESSAGE) {
            log.debug("heart beat");
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (((IdleStateEvent) evt).state() == IdleState.WRITER_IDLE) {
//                写空闲 发送一个ping 命令
                ctx.writeAndFlush(new PingMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                log.debug("write idle happened");
            }
        }else{
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("client catch exception：{}", cause.getMessage());
        ctx.close();
    }
}

package com.swz.rpc.transport.netty.handler;

import com.swz.rpc.container.BeanContainer;
import com.swz.rpc.container.utils.ClassUtils;
import com.swz.rpc.exception.RpcException;
import com.swz.rpc.pojo.Message;
import com.swz.rpc.pojo.PongMessage;
import com.swz.rpc.pojo.RequestMessage;
import com.swz.rpc.pojo.ResponseMessage;
import com.swz.rpc.registry.Registry;
import com.swz.rpc.registry.nacos.NacosRegistry;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;


/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private final BeanContainer container;

    public ServerHandler(){
        container = BeanContainer.getInstance();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg){
        if (msg.getMessageType() == Message.PING_MESSAGE) {
            //是ping心跳包 直接写回一个pong心跳包
            ctx.writeAndFlush(new PongMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }else if (msg.getMessageType() == Message.RPC_MESSAGE_TYPE_REQUEST){
//            是Rpc请求消息
            ResponseMessage responseMessage = new ResponseMessage();
            RequestMessage requestMessage = (RequestMessage) msg;
            responseMessage.setCodec(requestMessage.getCodec());
            responseMessage.setRequestId(requestMessage.getRequestId());
            if (ctx.channel().isActive() && ctx.channel().isWritable()){
                //从容器中找到服务对象 反射调用请求的方法 用响应消息包装返回值
                Class<?> interfaceClass = ClassUtils.loadClass(requestMessage.getInterfaceName());
                Set<Class<?>> classesBySuper = container.getClassesBySuper(interfaceClass);
                if (classesBySuper == null || classesBySuper.size() > 1 ) {
//                   未找到实现类或存在多个实现类
                    throw new RpcException("未找到实现类或存在多个实现类");
                }
                Object service = container.getBean(classesBySuper.iterator().next());
                try {
                    Method method = service.getClass().getMethod(requestMessage.getMethodName(), requestMessage.getParameterTypes());
                    Object returnValue = method.invoke(service, requestMessage.getParameterValue());
                    responseMessage.setReturnValue(returnValue);
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    log.error("远程调用出错",e);
                    responseMessage.setExceptionValue(new RpcException("远程调用出错"+e.getCause().getMessage()));
                }
            }else {
                log.error("当前Channel不可写，丢弃消息");
                responseMessage.setExceptionValue(new RpcException("远程调用失败"));
            }
            ctx.writeAndFlush(responseMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE){
//        客户端30秒内未发送请求  关闭连接
                log.debug("客户端30秒内未发送请求 关闭连接");
                ctx.close();
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }
}

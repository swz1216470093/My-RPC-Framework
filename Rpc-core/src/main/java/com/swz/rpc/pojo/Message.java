package com.swz.rpc.pojo;

import com.swz.rpc.config.RpcConfig;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
@Data
public abstract class Message implements Serializable {


    private static final long serialVersionUID = 5047031182007379935L;

    public Message() {
        codec = RpcConfig.CODEC;
    }

    /**
     * 根据消息类型字节，获得对应的消息 class
     * @param messageType 消息类型字节
     * @return 消息 class
     */
    public static Class<? extends Message> getMessageClass(int messageType) {
        return messageClasses.get(messageType);
    }

    private int messageType;

    private byte codec;

    public abstract int getMessageType();

    /**
     * 请求类型 byte 值
     */
    public static final byte RPC_MESSAGE_TYPE_REQUEST = 101;
    /**
     * 响应类型 byte 值
     */
    public static final byte  RPC_MESSAGE_TYPE_RESPONSE = 102;
    /**
     * 心跳包
     */
    public static final byte PING_MESSAGE = 103;
    public static final byte PONG_MESSAGE = 104;

    private static final Map<Byte, Class<? extends Message>> messageClasses = new HashMap<>();

    static {
        messageClasses.put(PING_MESSAGE,PingMessage.class);
        messageClasses.put(PONG_MESSAGE,PongMessage.class);
        messageClasses.put(RPC_MESSAGE_TYPE_REQUEST, RequestMessage.class);
        messageClasses.put(RPC_MESSAGE_TYPE_RESPONSE, ResponseMessage.class);
    }

}

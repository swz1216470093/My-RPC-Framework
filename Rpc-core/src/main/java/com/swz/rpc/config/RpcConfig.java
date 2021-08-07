package com.swz.rpc.config;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
public class RpcConfig {
    public static final int PORT = 9090;
    public static final byte VERSION = 1;
    public static final int MAX_FRAME_LENGTH = 8 * 1024;
    public static final byte[] MAGIC_NUMBER = {(byte) 'm', (byte) 'r', (byte) 'p', (byte) 'c'};
    public static final byte CODEC = SerializationTypeEnum.JDK.getCode();
}

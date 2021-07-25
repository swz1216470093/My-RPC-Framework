package com.swz.rpc.config;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.serializer.JdkSerializer;
import com.swz.rpc.serializer.JsonSerializer;
import com.swz.rpc.serializer.Serializer;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 向前走不回头
 */
@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {
    /**
     * JDK序列化
     */
    JDK((byte) 0x01, "jdk"),
    /**
     * Json序列化
     */
    JSON((byte) 0x02, "json");

    private final byte code;
    private final String name;


    public static String getName(byte code) {
        for (SerializationTypeEnum c : SerializationTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

    public static Serializer getSerializer(String name){
        if ("jdk".equals(name)){
            return new JdkSerializer();
        }else if ("json".equals(name)){
            return new JsonSerializer();
        }
        throw new RpcException("找不到序列化器");
    }
}

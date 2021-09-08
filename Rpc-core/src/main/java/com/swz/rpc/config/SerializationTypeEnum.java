package com.swz.rpc.config;

import com.swz.rpc.exception.RpcException;
import com.swz.rpc.serializer.Serializer;
import com.swz.rpc.serializer.impl.HessianSerializer;
import com.swz.rpc.serializer.impl.JdkSerializer;
import com.swz.rpc.serializer.impl.JsonSerializer;
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
    JSON((byte) 0x02, "json"),
    /**
     * Hessian序列化
     */
    HESSIAN((byte) 0x03, "hessian");
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
        }else if ("json".equals(name)) {
            return new JsonSerializer();
        } else if ("hessian".equals(name)) {
            return new HessianSerializer();
        }
        throw new RpcException("找不到序列化器");
    }
}

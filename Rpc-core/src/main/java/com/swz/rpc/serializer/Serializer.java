package com.swz.rpc.serializer;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
public interface Serializer {
    /***
     * 序列化 将对象序列化为字节数组
     * @param object
     * @return
     */
    public <T> byte[] serialize(T object);

    /**
     * 将字节数组反序列化为指定类型的对象
     * @param clazz
     * @param bytes
     * @return
     */
    public <T> T deserialize(Class<T> clazz,byte[] bytes);
}

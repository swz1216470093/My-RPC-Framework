package com.swz.rpc.serializer;

import com.swz.rpc.exception.RpcException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
@Slf4j
public class JdkSerializer implements Serializer{

    @Override
    public <T> byte[] serialize(T object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos);){
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            log.debug("序列化失败 {}",e.getMessage());
            throw new RpcException("序列化失败",e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try(ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bis);) {
            return (T)ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.debug("反序列化失败 {}",e.getMessage());
            throw new RpcException("反序列化失败",e);
        }
    }
}

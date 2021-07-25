package com.swz.rpc.proxy;

/**
 * 为service生成代理对象
 * @author 向前走不回头
 * @date 2021/7/24
 */
public interface ServiceProxy {
    /**
     * 得到代理对象
     * @param clazz
     * @return
     */
    <T> Object getProxy(Class<T> clazz);
}

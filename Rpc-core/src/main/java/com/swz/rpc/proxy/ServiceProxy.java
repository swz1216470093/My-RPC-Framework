package com.swz.rpc.proxy;

import java.util.concurrent.TimeUnit;

/**
 * 为service生成代理对象
 *
 * @author 向前走不回头
 * @date 2021/7/24
 */
public interface ServiceProxy {
    /**
     * 得到带超时的代理对象
     *
     * @param clazz
     * @param timeout
     * @param timeUnit
     * @return
     */
    <T> Object getProxyWithTimeout(Class<T> clazz, long timeout, TimeUnit timeUnit);

    /**
     * 得到代理对象 默认超时时间3s
     *
     * @param clazz
     * @return
     */
    <T> Object getProxy(Class<T> clazz);
}

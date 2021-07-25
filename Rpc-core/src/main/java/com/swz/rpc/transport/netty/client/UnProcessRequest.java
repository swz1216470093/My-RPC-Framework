package com.swz.rpc.transport.netty.client;

import io.netty.util.concurrent.Promise;

import java.time.format.SignStyle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class UnProcessRequest {
    private static final Map<String, Promise<Object>> PROMISE_MAP = new ConcurrentHashMap<>();
    static class Holder{
        private static final UnProcessRequest INSTANCE = new UnProcessRequest();
    }

    public void put(String requestId,Promise<Object> promise){
        PROMISE_MAP.put(requestId, promise);
    }
    public Promise<Object> remove(String requestId){
        return PROMISE_MAP.remove(requestId);
    }
    public static UnProcessRequest getInstance() {
        return Holder.INSTANCE;
    }
    private UnProcessRequest(){}
}

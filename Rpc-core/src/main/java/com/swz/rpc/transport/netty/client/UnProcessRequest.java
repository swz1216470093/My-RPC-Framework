package com.swz.rpc.transport.netty.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 向前走不回头
 * @date 2021/7/24
 */
public class UnProcessRequest {
    private static final Map<String, CompletableFuture<Object>> UN_PROCESSED_REQUEST_MAP = new ConcurrentHashMap<>();

    static class Holder {
        private static final UnProcessRequest INSTANCE = new UnProcessRequest();
    }

    public void put(String requestId, CompletableFuture<Object> future) {
        UN_PROCESSED_REQUEST_MAP.put(requestId, future);
    }

    public CompletableFuture<Object> remove(String requestId) {
        return UN_PROCESSED_REQUEST_MAP.remove(requestId);
    }

    public static UnProcessRequest getInstance() {
        return Holder.INSTANCE;
    }

    private UnProcessRequest() {
    }
}

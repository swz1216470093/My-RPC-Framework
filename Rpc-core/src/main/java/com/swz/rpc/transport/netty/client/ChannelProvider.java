package com.swz.rpc.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * @author 向前走不回头
 * @date 2021/7/25
 */
@Slf4j
public class ChannelProvider {
    private final Map<String, Channel> channelMap;
    private ChannelProvider(){
        channelMap = new ConcurrentHashMap<>();
    }
    public Channel get(InetSocketAddress address){
        String key = address.toString();
        log.debug("channel key  " +address.toString());
        if (channelMap.containsKey(key)) {
//          先从缓存中找
            Channel channel = channelMap.get(key);
//            Channel不为空且可用 直接返回 否则 删除缓存数据
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);
            }
        }
        return null;
    }
    public void put(InetSocketAddress address, Channel channel) {
        String key = address.toString();
        channelMap.put(key,channel);
    }
    static class Holder{
        private static final ChannelProvider INSTANCE = new ChannelProvider();
    }

    public static ChannelProvider getInstance() {
        return Holder.INSTANCE;
    }
}

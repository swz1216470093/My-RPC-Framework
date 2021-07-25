package com.swz.rpc.transport.netty.protocol;

import com.swz.rpc.config.RpcConfig;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
public class ProtocolDecoder extends LengthFieldBasedFrameDecoder {
    public ProtocolDecoder() {
        super(RpcConfig.MAX_FRAME_LENGTH, 8, 4);
    }
}

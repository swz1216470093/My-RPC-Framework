package com.swz.rpc.transport;

import com.swz.rpc.pojo.RequestMessage;

/**
 * 发送RPC请求
 * @author 向前走不回头
 * @date 2021/7/21
 */
public interface RpcTransport {
    /**
     * 发送RPC请求
     * @param requestMessage
     * @return
     */
    Object sendRpcRequest(RequestMessage requestMessage);

}

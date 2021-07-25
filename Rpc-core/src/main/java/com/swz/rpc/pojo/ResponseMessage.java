package com.swz.rpc.pojo;

import lombok.Data;
import lombok.ToString;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
@Data
@ToString(callSuper = true)
public class ResponseMessage extends Message {
    private static final long serialVersionUID = -5736395533086227120L;
    /**
     * 请求ID
     */
    private String requestId;
    /**
     * 返回值
     */
    private Object returnValue;
    /**
     * 异常值
     */
    private Exception exceptionValue;

    public ResponseMessage() {
        super();
    }

    @Override
    public int getMessageType() {
        return RPC_MESSAGE_TYPE_RESPONSE;
    }
}

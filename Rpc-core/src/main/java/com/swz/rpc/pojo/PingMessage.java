package com.swz.rpc.pojo;

import java.io.Serializable;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
public class PingMessage extends Message implements Serializable {
    private static final long serialVersionUID = 5979827706481481038L;

    public PingMessage() {
        super();
    }

    @Override
    public int getMessageType() {
        return PING_MESSAGE;
    }
}

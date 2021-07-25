package com.swz.rpc.pojo;

import java.io.Serializable;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
public class PongMessage extends Message implements Serializable {
    private static final long serialVersionUID = -4911258840762933128L;
    public PongMessage() {
        super();
    }
    @Override
    public int getMessageType() {
        return PONG_MESSAGE;
    }
}

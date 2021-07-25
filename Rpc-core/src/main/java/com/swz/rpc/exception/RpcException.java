package com.swz.rpc.exception;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
public class RpcException extends RuntimeException{

    private static final long serialVersionUID = 6429493234533731464L;

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
    public RpcException(String message) {
        super(message);
    }
    public RpcException(Throwable cause){
        super(cause);
    }
}

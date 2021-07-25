package com.swz.rpc.pojo;

import lombok.Getter;
import lombok.ToString;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
@Getter
@ToString(callSuper = true)
public class RequestMessage extends Message {

    private static final long serialVersionUID = 7121949141396877838L;
    /**
     * 调用的接口全限定名，服务端根据它找到实现
     */
    private String interfaceName;
    /**
     * 调用接口中的方法名
     */
    private String methodName;
    /**
     * 方法参数类型数组
     */
    private Class<?>[] parameterTypes;
    /**
     * 方法参数值数组
     */
    private Object[] parameterValue;
    /**
     * 请求ID
     */
    private String requestId;

    public RequestMessage(String  requestId, String interfaceName, String methodName, Class<?>[] parameterTypes, Object[] parameterValue) {
        super();
        this.requestId = requestId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameterValue = parameterValue;
    }

    @Override
    public int getMessageType() {
        return RPC_MESSAGE_TYPE_REQUEST;
    }
}

package com.swz.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**RPC组件标记注解（参考spring Component）
 *标记了RPCComponent注解的类都会被放入容器管理
 * @author 向前走不回头
 * @date 2021/7/24
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcComponent {
}

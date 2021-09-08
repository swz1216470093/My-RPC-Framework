package com.swz.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 自动注入注解
 *
 * @author 向前走不回头
 * @date 2021/7/24
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcAutowired {
    long timeout() default 3000;

    TimeUnit timeunit() default TimeUnit.MILLISECONDS;
}

package com.swz.rpc.container;

import com.swz.rpc.annotation.RpcAutowired;
import com.swz.rpc.container.utils.ClassUtils;
import com.swz.rpc.proxy.ServiceProxy;
import com.swz.rpc.proxy.jdk.JdkServiceProxy;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * @author 向前走不回头
 * @date 2021/8/7
 */
@Slf4j
public class DependencyInjector {
    /**
     * bean容器
     */
    private final BeanContainer beanContainer;
    private final ServiceProxy serviceProxy;

    public DependencyInjector() {
        beanContainer = BeanContainer.getInstance();
        serviceProxy = ServiceLoader.load(ServiceProxy.class).iterator().next();
    }

    public void doIoc() {
        Set<Class<?>> classes = beanContainer.getClasses();
        if (classes.isEmpty()) {
            log.warn("容器为空");
            return;
        }
//      遍历容器中所有class对象
        for (Class<?> clazz : classes) {
//            遍历class对象的所有成员变量
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
//        找出被autowire标记的成员变量
                if (field.isAnnotationPresent(RpcAutowired.class)) {
//                  得到成员变量的类型
                    Class<?> fieldType = field.getType();
//                    为这些成员变量生成代理
                    Object proxy = serviceProxy.getProxy(fieldType);
                    //        获取这些成员变量的类型在容器中的实例
//        通过反射将对应成员变量实例注入到成员变量所在类的实例里
                    Object targetBean = beanContainer.getBean(clazz);
                    ClassUtils.setField(field, targetBean, proxy, true);
                }
            }
        }
    }


    /**
     * 获得指定类型的实现类
     *
     * @return
     */
    private Class<?> getImplementClass(Class<?> fieldType, String autowiredValue) {
        Set<Class<?>> classSet = beanContainer.getClassesBySuper(fieldType);
        if (classSet == null) {
            return null;
        }
        if (!classSet.isEmpty()) {
            if ("".equals(autowiredValue) || autowiredValue == null) {
                if (classSet.size() == 1) {
//                    只有一个实现类
                    return classSet.iterator().next();
                } else {
//                有两个以上实现类且用户未指定其中一个实现类 则抛出异常
                    throw new RuntimeException(fieldType.getName() + "有多个实现类 请通过@RpcAutowired指定");
                }
            } else {
                for (Class<?> clazz : classSet) {
                    if (autowiredValue.equals(clazz.getSimpleName())) {
                        return clazz;
                    }
                }
            }
        }
        return null;
    }
}

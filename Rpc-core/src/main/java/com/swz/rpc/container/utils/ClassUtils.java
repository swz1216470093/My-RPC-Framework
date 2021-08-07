package com.swz.rpc.container.utils;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

/**
 * @author 向前走不回头
 * @date 2021/8/7
 */
@Slf4j
public class ClassUtils {
    /**
     * 通过反射实例化对象
     * @param clazz
     * @param access
     * @param <T>
     * @return
     */
    public static <T> T newInstance(Class<T> clazz,boolean access){
        try {
            Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
            declaredConstructor.setAccessible(access);
            return (T)declaredConstructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("实例化时发生异常",e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置类的属性值
     * @param field
     * @param target
     * @param value
     */
    public static void setField(Field field,Object target,Object value,boolean accessible){
        field.setAccessible(accessible);
        try {
            field.set(target,value);
        } catch (IllegalAccessException e) {
            log.error("属性赋值时异常");
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据类名加载class
     * @param className
     * @return
     */
    public static Class<?> loadClass(String className){
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("load class error:" ,e);
            throw new RuntimeException(e);
        }
    }
}

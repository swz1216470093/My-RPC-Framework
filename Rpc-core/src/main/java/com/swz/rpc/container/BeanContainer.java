package com.swz.rpc.container;

import com.swz.rpc.annotation.RpcComponent;
import com.swz.rpc.annotation.RpcService;
import com.swz.rpc.container.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 向前走不回头
 * @date 2021/8/7
 */
@Slf4j
public class BeanContainer {
    /**
     * 存放bean的map
     */
    private final ConcurrentHashMap<Class<?>, Object> beanMap = new ConcurrentHashMap<>();

    private BeanContainer() {
    }

    /**
     * 加载bean的注解列表
     */
    private final static List<Class<? extends Annotation>> BEAN_ANNOTATION =
            Arrays.asList(RpcComponent.class, RpcService.class);


    /**
     * 容器是否被加载过
     */
    private boolean loaded = false;

    /**
     * 判断容器是否加载
     *
     * @return
     */
    public boolean isLoaded() {
        return loaded;
    }

    public synchronized void loadBeans(String basePackage) {
//        判断容器是否已经加载过
        if (isLoaded()) {
            log.warn("容器已经加载过");
            return;
        }
        Reflections reflections = new Reflections(basePackage);
        for (Class<? extends Annotation> annotation : BEAN_ANNOTATION) {
            //查找basePackage包下标记了指定注解的类
            Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotation);
            for (Class<?> clazz : classes) {
//                将目标类为key 目标类为实例为value 放入容器中
                beanMap.put(clazz, ClassUtils.newInstance(clazz, true));
            }
        }
        loaded = true;
    }

    /**
     * bean实例的数量
     *
     * @return
     */
    public int size() {
        return beanMap.size();
    }

    /**
     * 添加一个class对象及其bean实例
     *
     * @param clazz
     * @param bean
     * @return
     */
    public Object putBean(Class<?> clazz, Object bean) {
        return beanMap.put(clazz, bean);
    }

    /**
     * 移除一个IOC容器管理的对象
     *
     * @param clazz
     * @return
     */
    public Object removeBean(Class<?> clazz) {
        return beanMap.remove(clazz);
    }

    /**
     * 根据class对象获取bean实例
     *
     * @param clazz
     * @return
     */
    public Object getBean(Class<?> clazz) {
        return beanMap.get(clazz);
    }

    /**
     * 获取所有class集合
     *
     * @return
     */
    public Set<Class<?>> getClasses() {
        return beanMap.keySet();
    }

    /**
     * 通过接口或父类获取实现类或子类的集合  不包括其本身
     *
     * @param interfaceOrClass
     * @return
     */
    public Set<Class<?>> getClassesBySuper(Class<?> interfaceOrClass) {
        Set<Class<?>> keySet = this.getClasses();
        if (keySet.isEmpty()) {
            log.warn("容器为空");
            return null;
        }
        Set<Class<?>> classSet = new HashSet<>();
//      判断容器中类是否为指定类的子类或实现类
        for (Class<?> clazz : keySet) {
            if (interfaceOrClass.isAssignableFrom(clazz) && clazz != interfaceOrClass) {
                classSet.add(clazz);
            }
        }
        return classSet.isEmpty() ? null : classSet;
    }

    /**
     * 获取容器中标记了指定注解的类
     * @param annotationClazz
     * @return
     */
    public Set<Class<?>>  getClassesAnnotatedWith(Class<? extends Annotation> annotationClazz){
        final Set<Class<?>> keySet = this.getClasses();
        HashSet<Class<?>> set = new HashSet<>();
        for (Class<?> clazz : keySet) {
            if (clazz.isAnnotationPresent(annotationClazz)){
                set.add(clazz);
            }
        }
        return set;
    }
    /**
     * 枚举单例
     */
    private enum ContainerHolder {
        Holder;
        private final BeanContainer instance;

        private ContainerHolder() {
            instance = new BeanContainer();
        }
    }

    public static BeanContainer getInstance() {
        return ContainerHolder.Holder.instance;
    }
}

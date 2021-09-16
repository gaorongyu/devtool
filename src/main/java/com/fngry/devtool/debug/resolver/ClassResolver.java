package com.fngry.devtool.debug.resolver;

import org.springframework.aop.framework.AdvisedSupport;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * class解析
 * @author gaorongyu
 */
public class ClassResolver {

    private static final String ENHANCER_BY_CGLIB = "EnhancerByCGLIB";

    private static final String ENHANCER_BY_SPRING_CGLIB = "EnhancerBySpringCGLIB";

    private static final String JDK_DYNAMIC_AOP_PROXY = "org.springframework.aop.framework.JdkDynamicAopProxy";

    private static final String ADVISED = "advised";

    /**
     * 获取类的所有方法 包含父类的方法
     * @param clazz
     * @return
     */
    public static List<Method> getMethods(Class clazz) {
        List<Method> methodList = new ArrayList<>();
        while (clazz != Object.class) {
            Method[] methods = clazz.getDeclaredMethods();
            methodList.addAll(Arrays.asList(methods));
            clazz = clazz.getSuperclass();
        }
        return methodList;
    }


    /**
     * 获取类的所有属性 包含父类的属性
     * @param clazz
     * @return
     */
    public static List<Field> getFields(Class clazz) {
        List<Field> fieldList = new ArrayList<>();
        while (clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();
            fieldList.addAll(Arrays.asList(fields));
            clazz = clazz.getSuperclass();
        }
        return fieldList;
    }

    /**
     * 获取目标类 如果被代理过则找到原始类
     * @param bean
     * @return
     * @throws Exception
     */
    public static Class getTargetClassOfBean(Object bean) throws Exception {
        Class clazz = bean.getClass();
        Class targetClazz = null;

        if (clazz.getName().contains(ENHANCER_BY_CGLIB)
                || clazz.getName().contains(ENHANCER_BY_SPRING_CGLIB)) {
            // cglib增强的类
            targetClazz = clazz.getSuperclass();
        } else if (Proxy.isProxyClass(clazz)) {
            // 被代理过的类
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(bean);
            Class proxyClass = invocationHandler.getClass();

            if (JDK_DYNAMIC_AOP_PROXY.equals(proxyClass.getName())) {
                Field field = proxyClass.getField(ADVISED);
                field.setAccessible(true);
                AdvisedSupport advisedSupport = (AdvisedSupport) field.get(invocationHandler);
                Object targetObject = advisedSupport.getTargetSource().getTarget();

                targetClazz = targetObject.getClass();
            }
        } else {
            targetClazz = bean.getClass();
        }
        return targetClazz;
    }

    public static List<String> getGenericParameterNames(Class<?> clazz) {
        String genericString = clazz.toGenericString();
        int startIndex = genericString.indexOf('<');
        int endIndex = genericString.indexOf('>');
        String typeNameString = genericString.substring(startIndex + 1, endIndex);
        return Arrays.asList(typeNameString.split(","));
    }
    
}

package com.fngry.devtool.debug;

import com.fngry.devtool.debug.metadata.MethodMetaData;
import com.fngry.devtool.debug.resolver.ClassResolver;
import com.fngry.devtool.debug.resolver.MethodResolver;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author gaorongyu
 */
@Service
public class DevTestExecutor implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取bean的所有方法签名 包含父类的方法
     * @param beanName
     * @return
     * @throws Exception
     */
    public List<MethodMetaData> getMethodMetasForBean(String beanName) throws Exception {
        Object bean = this.getBean(beanName);
        Class clazz = ClassResolver.getTargetClassOfBean(bean);
        return this.getMethodMetas(clazz);
    }

    /**
     * 获取class的所有方法签名 包含父类的方法
     * @param className
     * @return
     * @throws Exception
     */
    public List<MethodMetaData> getMethodMetasForClass(String className) throws Exception {
        Class clazz = Class.forName(className);
        return this.getMethodMetas(clazz);
    }

    /**
     * 生成方法参数模版
     * @param beanName
     * @param signature
     * @return
     * @throws Exception
     */
    public Map<String, Object> methodParameterTemplateForBean(String beanName, String signature) throws Exception {
        Object bean = this.getBean(beanName);
        Method method = this.findMethodFromBean(bean, signature);
        return MethodResolver.methodParameterTemplate(method);
    }

    /**
     * 生成方法参数模版
     * @param className
     * @param signature
     * @return
     * @throws Exception
     */
    public Map<String, Object> methodParameterTemplateForClass(String className, String signature) throws Exception {
        Class clazz = Class.forName(className);
        Method method = this.findMethodFromClass(clazz, signature);
        return MethodResolver.methodParameterTemplate(method);
    }

    /**
     * 执行bean的方法
     * @param beanName
     * @param methodSignature
     * @param paramMap
     * @return
     * @throws Exception
     */
    public Object executeMethodForBean(String beanName, String methodSignature, Map<String, String> paramMap) throws Exception {
        Object bean = this.getBean(beanName);
        Method method = this.findMethodFromBean(bean, methodSignature);
        Object[] params = MethodResolver.bindParameters(paramMap, method);

        if (!Modifier.isPublic(method.getModifiers())) {
            // 非public方法 不经过Spring代理 直接调用对象方法
            method.setAccessible(true);
            Object obj = AopTargetUtils.getTarget(bean);
            return method.invoke(obj, params);
        }
        return method.invoke(bean, params);
    }

    /**
     * 执行普通对象的方法
     * @param className
     * @param methodSignature
     * @param paramMap
     * @return
     * @throws Exception
     */
    public Object executeMethodForClass(String className, String methodSignature, Map<String, String> paramMap) throws Exception {
        Class clazz = Class.forName(className);
        Object obj = clazz.newInstance();
        Method method = this.findMethodFromClass(clazz, methodSignature);
        Object[] params = MethodResolver.bindParameters(paramMap, method);
        method.setAccessible(true);
        return method.invoke(obj, params);
    }

    /**
     * 查找bean的方法
     * @param bean
     * @param signature
     * @return
     * @throws Exception
     */
    private Method findMethodFromBean(Object bean, String signature) throws Exception {
        return this.findMethodFromClass(ClassResolver.getTargetClassOfBean(bean), signature);
    }

    private Method findMethodFromClass(Class clazz, String signature) {
        return BeanUtils.resolveSignature(signature, clazz);
    }

    private Object getBean(String beanName) {
        Object obj = applicationContext.getBean(beanName);
        if (obj == null) {
            throw new DebugException("can not find bean by name: " + beanName);
        }
        return obj;
    }

    /**
     * 获取类的所有方法签名
     * @param clazz
     * @return
     * @throws Exception
     */
    private List<MethodMetaData> getMethodMetas(Class clazz) {
        List<Method> methodList = ClassResolver.getMethods(clazz);
        return methodList.stream()
                .filter(this::canInvoke)
                .map(MethodResolver::getMethodMetadata).collect(Collectors.toList());
    }

    private boolean canInvoke(Method method) {
        // 排除lambda表达式生成的方法
        return !MethodResolver.isAutoGeneratedLambdaMethod(method);
    }

}

package com.fngry.devtool.debug.resolver;

import com.fngry.devtool.debug.metadata.MethodMetaData;
import com.fngry.devtool.Constants;
import com.fngry.devtool.debug.DebugException;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 方法解析
 * @author gaorongyu
 */
public class MethodResolver {

    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    public static MethodMetaData getMethodMetadata(Method method) {
        MethodMetaData meta = new MethodMetaData();
        meta.setSignature(MethodResolver.getMethodSignature(method));
        meta.setDeclaration(MethodResolver.getMethodDeclaration(method));
        return meta;
    }
        /**
         * 生成方法的签名
         * @param method
         * @return
         */
    public static String getMethodSignature(Method method) {
        StringBuilder signature = new StringBuilder();
        signature.append(method.getName()).append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            signature.append(Arrays.stream(parameterTypes).map(Class::getName).collect(Collectors.joining(", ")));
        }
        signature.append(")");
        return signature.toString();
    }

    public static String getMethodDeclaration(Method method) {
        StringBuilder declaration = new StringBuilder();
        // 修饰符
        declaration.append(accessModifier(method.getModifiers()));
        // 返回值
        Type returnType = method.getGenericReturnType();
        String returnTypeSimpleName = getTypeSimpleName(returnType);
        declaration.append(" ").append(returnTypeSimpleName);
        // 方法名
        declaration.append(" ").append(method.getName()).append("(");
        // 参数
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        Type[] parameterTypes = method.getGenericParameterTypes();
        if (parameterNames != null && parameterNames.length > 0) {
            for (int i = 0; i < parameterTypes.length; i++) {
                declaration.append(MethodResolver.getTypeSimpleName(parameterTypes[i]))
                        .append(" ").append(parameterNames[i]);
                if (i < parameterTypes.length - 1) {
                    declaration.append(", ");
                }
            }
        }
        declaration.append(")");
        // throws
        Class<?>[] exceptionClasses = method.getExceptionTypes();
        if (exceptionClasses.length > 0) {
            declaration.append(" throws ");
            declaration.append(Arrays.stream(exceptionClasses)
                    .map(MethodResolver::getTypeSimpleName).collect(Collectors.joining(", ")));
        }
        return declaration.toString();
    }

    /**
     * 生成方法参数模版
     * @param method
     * @return
     * @throws Exception
     */
    public static Map<String, Object> methodParameterTemplate(Method method) throws Exception {
        Map<String, Object> template = new HashMap<>(Constants.COLLECTION_INIT_SIZE);
        Class<?>[] parameterClasses = method.getParameterTypes();
        Type[] parameterTypes = method.getGenericParameterTypes();
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
        if (parameterNames != null && parameterNames.length > 0) {
            for (int i = 0; i < parameterNames.length; i++) {
                Object defaultValue = ParameterResolver.defaultValue(null, parameterClasses[i], parameterTypes[i]);
                template.put(parameterNames[i], defaultValue);
            }
        }
        return template;
    }


    /**
     * 参数绑定
     * @param paramMap
     * @param method
     * @return
     */
    public static Object[] bindParameters(Map<String, String> paramMap, Method method) {
        List<Object> paramObjectList = new ArrayList<>();
        Type[] parameterTypes = method.getGenericParameterTypes();
        Class<?>[] parameterClasses = method.getParameterTypes();
        String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String objString = paramMap.get(parameterNames[i]);
                Object objValue = ParameterResolver.parse(objString, parameterClasses[i], parameterTypes[i]);
                paramObjectList.add(objValue);
            }
        }
        return paramObjectList.toArray();
    }

    public static String getTypeSimpleName(Type type) {
        if (Class.class == type.getClass()) {
            return ((Class<?>) type).getSimpleName();
        } else if (ParameterizedTypeImpl.class == type.getClass()) {
            return getGenericTypeSimpleName((ParameterizedTypeImpl) type);
        } else if (TypeVariableImpl.class ==  type.getClass()) {
            return type.getTypeName();
        } else {
            throw new DebugException("unsupported type: " + type);
        }
    }

    /**
     * 获取范行类型的简单名称 例如java.util.List<java.lang.String>简化成List<String>
     * @param parameterizedType
     * @return
     */
    public static String getGenericTypeSimpleName(ParameterizedTypeImpl parameterizedType) {
        Class<?> rawType = parameterizedType.getRawType();
        Type[] actualTypes = parameterizedType.getActualTypeArguments();

        StringBuilder simpleName = new StringBuilder();
        simpleName.append(rawType.getSimpleName());

        if (actualTypes.length > 0) {
            simpleName.append("<");
            String actualTypeSimpleNames = Arrays.stream(actualTypes)
                    .map(MethodResolver::getTypeSimpleName)
                    .collect(Collectors.joining(", "));
            simpleName.append(actualTypeSimpleNames);
            simpleName.append(">");
        }
        return simpleName.toString();
    }

    public static String accessModifier(int modifiers) {
        String accessModifier;
        if (Modifier.isPublic(modifiers)) {
            accessModifier = "public";
        } else if (Modifier.isProtected(modifiers)) {
            accessModifier = "protected";
        } else if (Modifier.isPrivate(modifiers)) {
            accessModifier = "private";
        } else {
            accessModifier = "";
        }
        if (Modifier.isStatic(modifiers)) {
            accessModifier = accessModifier + " static";
        }
        return accessModifier;
    }

    /**
     * lambda表达式生成的方法
     * @param method
     * @return
     */
    public static boolean isAutoGeneratedLambdaMethod(Method method) {
        return Modifier.isPrivate(method.getModifiers()) && method.getName().contains("$");
    }

}

package com.fngry.devtool.debug.resolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fngry.devtool.Constants;
import com.fngry.devtool.debug.DebugException;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 参数解析器
 * @author gaorongyu
 */
public class ParameterResolver {

    private static List<Class<?>> LET_IT_NULL_CLASSES = Arrays.asList(Long.class, Integer.class);

    /**
     * 解析参数
     * @param objString
     * @param parameterClass
     * @param parameterType
     * @return
     */
    public static Object parse(String objString, Class parameterClass, Type parameterType) {
        if (objString == null || objString.trim().length() == 0) {
            return null;
        }
        Object objValue;
        if (Long.class == parameterClass || long.class == parameterClass) {
            return Long.parseLong(objString);
        } else if (Integer.class == parameterClass || int.class == parameterClass) {
            return Integer.parseInt(objString);
        } else if (BigDecimal.class == parameterClass) {
            return new BigDecimal(objString);
        } else if (Boolean.class == parameterClass || boolean.class == parameterClass) {
            return Boolean.parseBoolean(objString);
        } else if (parameterClass == String.class) {
            objValue = objString;
        } else {
            objValue = JSON.parseObject(objString, parameterType);
        }
        return objValue;
    }

    /**
     * 参数默认值
     * @param clazz
     * @param type
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static Object defaultValue(ClassNode parentNode, Class<?> clazz, Type type) throws InstantiationException, IllegalAccessException {
        if (parentNode == null) {
            parentNode = new ClassNode(clazz, null);
        }
        if (Long.class == clazz || long.class == clazz) {
            return 0L;
        } else if (Integer.class == clazz || int.class == clazz) {
            return 0;
        } else if (BigDecimal.class == clazz) {
            return BigDecimal.ZERO;
        } else if (Boolean.class == clazz || boolean.class == clazz) {
            return false;
        } else if (String.class == clazz) {
            return null;
        } else if (Date.class ==  clazz) {
            return null;
        } else if (Timestamp.class == clazz) {
            return null;
        } else if (Class.class == clazz) {
            return null;
        } else if (clazz.isEnum()) {
            return null;
        } else if (List.class.isAssignableFrom(clazz) || clazz.isArray()) {
            return arrayDefaultValue(parentNode, clazz, type);
        } else if (Map.class.isAssignableFrom(clazz)) {
            return new HashMap<>(Constants.COLLECTION_INIT_SIZE);
        } else {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())
                    || clazz == Object.class) {
                return null;
            }
            Object instance = clazz.newInstance();
            setFieldsDefaultValue(parentNode, clazz, type, instance);
            return instance;
        }
    }

    private static void setFieldsDefaultValue(ClassNode parentNode, Class<?> targetClazz, Type targetType, Object target) throws InstantiationException, IllegalAccessException {
        List<Field> fields = ClassResolver.getFields(targetClazz);
        List<Field> candidateFields = fields.stream().filter(e -> candidateField(e, target))
                .collect(Collectors.toList());

        for (Field field : candidateFields) {
            Class<?> fieldClass = field.getType();
            Type fieldGenericType = field.getGenericType();
            Type fieldType = fieldGenericType;

            if (fieldGenericType instanceof TypeVariableImpl) {
                // 泛型类型 声明为 T t; 的格式 找到类声明 <ActualClass> 泛型的具体类
                List<String> genericTypeNames = ClassResolver.getGenericParameterNames(targetClazz);
                Type[] actualTypes = ((ParameterizedTypeImpl) targetType).getActualTypeArguments();
                for (int i = 0; i < actualTypes.length; i++) {
                    String genericTypeName = genericTypeNames.get(i);
                    if (fieldGenericType.getTypeName().equals(genericTypeName)) {
                        fieldType = actualTypes[i];
                        fieldClass = (Class<?>) actualTypes[i];
                    }
                }
            }
            boolean isCycle = checkCycle(parentNode, fieldClass);
            if (!isCycle) {
                ClassNode classNode = new ClassNode(fieldClass, parentNode);
                Object fieldValue = defaultValue(classNode, fieldClass, fieldType);
                setValueForField(field, target, fieldValue);
            }
        }
    }

    private static boolean candidateField(Field field, Object target) {
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        if (isStatic) {
            return false;
        }
        if (letItNull(field.getType())) {
            return false;
        }
        field.setAccessible(true);
        Object fieldValue;
        try {
            fieldValue = field.get(target);
        } catch (IllegalAccessException e) {
            throw new DebugException(e);
        }
        return fieldValue == null;
    }

    private static void setValueForField(Field field, Object target, Object fieldValue) {
        field.setAccessible(true);
        try {
            field.set(target, fieldValue);
        } catch (IllegalAccessException e) {
            throw new DebugException(e);
        }
    }

    private static Object arrayDefaultValue(ClassNode parentNode, Class<?> clazz, Type type) throws InstantiationException, IllegalAccessException {
        String jsonExpress = "[]";
        if (type instanceof ParameterizedTypeImpl) {
            // List<AObject> 类型给AObject初始化一个值
            Type[] actualTypes = ((ParameterizedTypeImpl) type).getActualTypeArguments();
            Type actualType = actualTypes[0];

            if (actualType instanceof Class) {
                Class<?> actualClass = (Class<?>) actualType;
                boolean isCycle = checkCycle(parentNode, actualClass);
                boolean letItNull = letItNull(actualClass);
                if (!isCycle && !letItNull) {
                    ClassNode classNode = new ClassNode(actualClass, parentNode);
                    Object actualValue = defaultValue(classNode, actualClass, actualType);
                    if (actualValue != null) {
                        jsonExpress = "[" + JSON.toJSONString(actualValue, SerializerFeature.WriteMapNullValue) + "]";
                    }
                }
            }
        }
        return JSON.parseObject(jsonExpress, type);
    }

    private static boolean checkCycle(ClassNode parentNode, Class<?> clazz) {
        if (parentNode == null) {
            return false;
        }
        while (parentNode != null) {
            if (parentNode.getClazz() == clazz) {
                return true;
            }
            parentNode = parentNode.getParent();
        }
        return false;
    }

    private static boolean letItNull(Class<?> clazz) {
        return LET_IT_NULL_CLASSES.contains(clazz);
    }

    private static class ClassNode {
        private Class<?> clazz;
        private ClassNode parent;

        public ClassNode(Class<?> clazz, ClassNode parentNode) {
            this.clazz = clazz;
            this.parent = parentNode;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }

        public ClassNode getParent() {
            return parent;
        }

        public void setParent(ClassNode parent) {
            this.parent = parent;
        }
    }

}

package com.fngry.devtool.debug.endpoint;

import java.util.Map;

/**
 * @author gaorongyu
 */
public class ExecuteParameter {

    /**
     * SpringBean or OrdinaryClass
     */
    private String objectType;

    private String objectName;

    private String methodSignature;

    private Map<String, String> paramMap;

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public Map<String, String> getParamMap() {
        return paramMap;
    }

    public void setParamMap(Map<String, String> paramMap) {
        this.paramMap = paramMap;
    }
}

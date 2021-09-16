package com.fngry.devtool.debug.metadata;

/**
 * 方法元数据
 * @author gaorongyu
 */
public class MethodMetaData {

    /**
     * 方法签名
     *   格式 pageQuery(com.whatsegg.crown.domain.purchase.query.PurchaseOrderPageQuery)
     */
    private String signature;

    /**
     * 方法声明
     *   格式 public List<PurchaseOrderBo> pageQuery(PurchaseOrderPageQuery query)
     */
    private String declaration;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getDeclaration() {
        return declaration;
    }

    public void setDeclaration(String declaration) {
        this.declaration = declaration;
    }

    @Override
    public String toString() {
        return "MethodMeta{" +
                "signature='" + signature + '\'' +
                ", declaration='" + declaration + '\'' +
                '}';
    }

}

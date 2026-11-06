package com.tivarek.week10;
import java.math.BigDecimal;
public record ProductView(long productId,String sku,String name,long categoryId,String categoryName,
    Long brandId,String brandName,long unitId,String unitName,String stockMode,BigDecimal listPrice,
    String description,boolean active,int revision) {
    public ProductInput input() { return new ProductInput(sku,name,categoryId,brandId,unitId,stockMode,listPrice,description,revision); }
}

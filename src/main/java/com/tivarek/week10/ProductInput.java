package com.tivarek.week10;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Locale;
public record ProductInput(
    @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}",message="SKU dài 1–40 ký tự: chữ, số, dấu chấm, gạch ngang hoặc gạch dưới") String sku,
    @NotBlank(message="Nhập tên sản phẩm") @Size(max=180) String name,
    @NotNull @Positive Long categoryId,
    @Positive Long brandId,
    @NotNull @Positive Long unitId,
    @NotNull @Pattern(regexp="SERIALIZED|BULK",message="Chọn SERIALIZED hoặc BULK") String stockMode,
    @NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal listPrice,
    @Size(max=2000) String description,
    @NotNull @PositiveOrZero Integer revision) {
    public ProductInput {
        sku=sku==null?null:sku.strip().toUpperCase(Locale.ROOT);
        name=name==null?null:name.strip();
        description=description==null?null:description.strip();
    }
    public static ProductInput empty() { return new ProductInput("","",null,null,null,"SERIALIZED",BigDecimal.ZERO,"",0); }
}

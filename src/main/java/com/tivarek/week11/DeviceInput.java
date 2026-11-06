package com.tivarek.week11;
import java.math.BigDecimal;
import java.util.*;
import jakarta.validation.constraints.*;
public record DeviceInput(
    @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}",message="Mã máy dài 1–40 ký tự: chữ, số, chấm, gạch ngang hoặc gạch dưới") String assetTag,
    @NotNull @Positive Long productId,
    @Size(max=100) String manufacturerSerial,
    @NotBlank(message="Nhập mô tả thiết bị") @Size(max=2000) String description,
    @NotBlank @Pattern(regexp="NEW|LIKE_NEW|GOOD|FAIR|FAULTY",message="Chọn tình trạng hợp lệ") String conditionGrade,
    @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal askingPrice,
    @Size(max=2000) String faultDescription,
    @Size(max=16) String version,
    @Size(max=40) Map<String,String> specifications) {
    public DeviceInput {
        assetTag=assetTag==null?null:assetTag.strip().toUpperCase(Locale.ROOT);
        manufacturerSerial=manufacturerSerial==null||manufacturerSerial.isBlank()?null:manufacturerSerial.strip();
        description=description==null?null:description.strip();
        faultDescription=faultDescription==null?null:faultDescription.strip();
        specifications=specifications==null?new LinkedHashMap<>():new LinkedHashMap<>(specifications);
    }
    public static DeviceInput empty() { return new DeviceInput("",null,null,"","GOOD",null,"","",Map.of()); }
}

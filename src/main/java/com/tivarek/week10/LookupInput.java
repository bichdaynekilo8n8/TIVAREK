package com.tivarek.week10;
import jakarta.validation.constraints.*;
import java.util.Locale;
public record LookupInput(
    @NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}",message="Mã dài 1–40 ký tự: chữ, số, chấm, gạch ngang hoặc gạch dưới") String code,
    @NotBlank(message="Nhập tên") @Size(max=180) String name,
    @NotNull @PositiveOrZero Integer revision) {
    public LookupInput { code=code==null?null:code.strip().toUpperCase(Locale.ROOT);name=name==null?null:name.strip(); }
    public static LookupInput empty() { return new LookupInput("","",0); }
}

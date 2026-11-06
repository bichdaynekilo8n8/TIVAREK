package com.tivarek.week11;
import jakarta.validation.constraints.*;
public record CheckInput(@NotBlank @Size(max=2000) String findings,
    @NotNull @Pattern(regexp="PASS|FAIL|NEEDS_REPAIR") String result,
    @NotBlank @Pattern(regexp="[0-9a-fA-F]{16}") String version) {
    public CheckInput { findings=findings==null?null:findings.strip(); }
}

package com.tivarek.week8;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AddressInput(
        @NotBlank @Size(max=250) String addressLine,
        @NotBlank @Size(max=120) String wardText,
        @NotBlank @Size(max=120) String provinceText) {}

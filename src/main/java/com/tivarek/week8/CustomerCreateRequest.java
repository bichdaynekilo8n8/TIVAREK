package com.tivarek.week8;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record CustomerCreateRequest(
        @NotBlank @Pattern(regexp="[A-Za-z0-9-]{3,40}") String customerCode,
        @NotBlank @Size(max=180) String fullName,
        @NotBlank @Pattern(regexp="\\+?[0-9]{8,15}") String phone,
        @Email @Size(max=254) String email,
        @Valid AddressInput address) {}

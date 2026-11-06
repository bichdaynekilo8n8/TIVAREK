package com.tivarek.week8;
import jakarta.validation.constraints.*;
public record CustomerUpdateRequest(
        @NotBlank @Size(max=180) String fullName,
        @NotBlank @Pattern(regexp="\\+?[0-9]{8,15}") String phone,
        @Email @Size(max=254) String email) {}

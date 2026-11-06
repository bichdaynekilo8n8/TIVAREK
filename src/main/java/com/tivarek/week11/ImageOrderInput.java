package com.tivarek.week11;
import java.util.List;
import jakarta.validation.constraints.*;
public record ImageOrderInput(@NotNull @Size(min=1,max=10) List<@NotNull @Positive Long> imageIds,
    @NotBlank @Pattern(regexp="[0-9a-fA-F]{16}") String version) {}

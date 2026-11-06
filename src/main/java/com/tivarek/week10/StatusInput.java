package com.tivarek.week10;
import jakarta.validation.constraints.*;
public record StatusInput(@NotNull Boolean active,@NotNull @PositiveOrZero Integer revision) {}

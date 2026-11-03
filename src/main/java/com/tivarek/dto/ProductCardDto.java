package com.tivarek.dto;

import java.math.BigDecimal;

public record ProductCardDto(
        Long id,
        String name,
        BigDecimal price
) {
}
package com.tivarek.week12;

import java.math.BigDecimal;

/** One immutable movement request. Positive quantity; adjustment sign is controlled by direction. */
public record StockCommand(String type, String itemType, Long productId, Long deviceId,
                           Long fromLocationId, Long toLocationId, int quantity,
                           BigDecimal unitCost, Long supplierId, Long saleLineId,
                           String reason) { }

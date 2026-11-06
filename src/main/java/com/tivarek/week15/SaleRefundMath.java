package com.tivarek.week15;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Sale lines have a discount for the entire line, not a per-unit discount.
 * Prorate the original after-discount, after-tax line total by the cumulative
 * number of returned items. Round DOWN for partial returns to avoid ever
 * exceeding the invoiced line total. A fully returned line refunds exactly its
 * original two-decimal gross amount.
 */
public final class SaleRefundMath {
    private SaleRefundMath() { }

    public static BigDecimal maximumRefundable(BigDecimal unitPrice, int orderedQuantity,
                                                BigDecimal lineDiscount, BigDecimal taxRate,
                                                int returnedQuantity) {
        if (unitPrice == null || lineDiscount == null || taxRate == null
                || orderedQuantity <= 0 || returnedQuantity < 0 || returnedQuantity > orderedQuantity
                || unitPrice.signum() < 0 || lineDiscount.signum() < 0
                || taxRate.signum() < 0 || taxRate.compareTo(new BigDecimal("100")) > 0) {
            throw new SaleProblem("Dữ liệu số lượng hoặc giá trị dòng bán không hợp lệ.");
        }
        BigDecimal net = unitPrice.multiply(BigDecimal.valueOf(orderedQuantity)).subtract(lineDiscount);
        if (net.signum() < 0) throw new SaleProblem("Chiết khấu vượt giá trị dòng bán.");
        BigDecimal tax = net.multiply(taxRate.movePointLeft(2)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = net.add(tax).setScale(2, RoundingMode.HALF_UP);
        if (returnedQuantity == orderedQuantity) return gross;
        return gross.multiply(BigDecimal.valueOf(returnedQuantity))
                .divide(BigDecimal.valueOf(orderedQuantity), 2, RoundingMode.DOWN);
    }
}

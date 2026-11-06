package com.tivarek.week20;

import com.tivarek.week15.SaleProblem;
import com.tivarek.week15.SaleRefundMath;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class Week20RefundMathTest {
    private static BigDecimal value(String text) { return new BigDecimal(text); }

    @Test void lineWideDiscountIsNotMultipliedByEachReturnedUnit() {
        // 3 x 100 - one line discount of 30 = 270; 10% tax = 27; total = 297.
        assertEquals(value("99.00"),SaleRefundMath.maximumRefundable(
                value("100.00"),3,value("30.00"),value("10.00"),1));
        assertEquals(value("198.00"),SaleRefundMath.maximumRefundable(
                value("100.00"),3,value("30.00"),value("10.00"),2));
        assertEquals(value("297.00"),SaleRefundMath.maximumRefundable(
                value("100.00"),3,value("30.00"),value("10.00"),3));
    }

    @Test void cumulativePartialRefundNeverExceedsOriginalTotal() {
        assertEquals(value("3.33"),SaleRefundMath.maximumRefundable(
                value("10.00"),3,value("20.00"),BigDecimal.ZERO,1));
        assertEquals(value("6.66"),SaleRefundMath.maximumRefundable(
                value("10.00"),3,value("20.00"),BigDecimal.ZERO,2));
        assertEquals(value("10.00"),SaleRefundMath.maximumRefundable(
                value("10.00"),3,value("20.00"),BigDecimal.ZERO,3));
    }

    @Test void fullReturnUsesRoundedTaxOnce() {
        assertEquals(value("0.12"),SaleRefundMath.maximumRefundable(
                value("0.11"),1,BigDecimal.ZERO,value("10.00"),1));
    }

    @Test void zeroReturnIsZeroAndMalformedLinesAreRejected() {
        assertEquals(value("0.00"),SaleRefundMath.maximumRefundable(
                value("250.00"),2,value("10.00"),BigDecimal.ZERO,0));
        assertThrows(SaleProblem.class,()->SaleRefundMath.maximumRefundable(
                value("100.00"),2,value("300.00"),BigDecimal.ZERO,1));
        assertThrows(SaleProblem.class,()->SaleRefundMath.maximumRefundable(
                value("100.00"),2,BigDecimal.ZERO,BigDecimal.ZERO,3));
    }
}

package com.tivarek.week20;

import com.tivarek.week12.InventoryService;
import com.tivarek.week15.SaleProblem;
import com.tivarek.week15.SaleWorkflowService;
import com.tivarek.week15.SalesService;
import com.tivarek.week16.WarrantyIssuanceService;
import com.tivarek.week8.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Week20PaymentIdempotencyTest {
    private static final String KEY = "week20-idempotent-01";

    private static SaleWorkflowService service(JdbcTemplate db) {
        return new SaleWorkflowService(db, mock(SalesService.class), mock(InventoryService.class),
                mock(AuditService.class), mock(WarrantyIssuanceService.class));
    }

    private static void orderIsConfirmed(JdbcTemplate db) {
        when(db.queryForList(startsWith("SELECT OrderCode,Status FROM dbo.SalesOrders"),eq(7L)))
                .thenReturn(List.of(Map.of("OrderCode","SO-TEST", "Status","CONFIRMED")));
    }

    @Test void reusedPaymentKeyCannotSilentlyAcceptAnotherBankReference() {
        var db=mock(JdbcTemplate.class);
        orderIsConfirmed(db);
        when(db.queryForList(startsWith("SELECT PaymentID,Amount,Method,Direction,Status,ProviderReference"),eq(KEY)))
                .thenReturn(List.of(Map.of("PaymentID",22L,"Amount",new BigDecimal("450000.00"),
                        "Method","BANK_TRANSFER","Direction","IN","Status","SUCCEEDED",
                        "ProviderReference","BANK-OLD")));

        assertThrows(SaleProblem.class,()->service(db).payment(7L,new BigDecimal("450000.00"),
                "BANK_TRANSFER","BANK-NEW",KEY));
    }

    @Test void reusedRefundKeyCannotSilentlyAcceptAnotherReference() {
        var db=mock(JdbcTemplate.class);
        orderIsConfirmed(db);
        when(db.queryForList(startsWith("SELECT PaymentID,Amount,Method,Direction,Status,ProviderReference"),eq(KEY)))
                .thenReturn(List.of(Map.of("PaymentID",26L,"Amount",new BigDecimal("250000.00"),
                        "Method","CASH","Direction","OUT","Status","SUCCEEDED",
                        "ProviderReference","RECEIPT-OLD")));

        assertThrows(SaleProblem.class,()->service(db).refund(7L,22L,new BigDecimal("250000.00"),
                "CASH","RECEIPT-NEW",KEY));
    }

    @Test void bankTransferRequiresVerifiedReferenceEvenWhenKeyIsReused() {
        var db=mock(JdbcTemplate.class);
        orderIsConfirmed(db);
        assertThrows(SaleProblem.class,()->service(db).payment(7L,new BigDecimal("1000.00"),
                "BANK_TRANSFER",null,KEY));
    }
}

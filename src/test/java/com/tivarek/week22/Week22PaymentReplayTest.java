package com.tivarek.week22;

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

class Week22PaymentReplayTest {
    private static final String KEY="W22-payment-replay";

    private static SaleWorkflowService service(JdbcTemplate db) {
        return new SaleWorkflowService(db,mock(SalesService.class),mock(InventoryService.class),
            mock(AuditService.class),mock(WarrantyIssuanceService.class));
    }

    private static void previouslyPaid(JdbcTemplate db) {
        when(db.queryForList(startsWith("SELECT OrderCode,Status FROM dbo.SalesOrders"),eq(8L)))
            .thenReturn(List.of(Map.of("OrderCode","SO-W22","Status","CONFIRMED")));
        when(db.queryForList(startsWith("SELECT PaymentID,Amount,Method,Direction,Status,ProviderReference"),eq(KEY)))
            .thenReturn(List.of(Map.of("PaymentID",42L,"Amount",new BigDecimal("200000.00"),
                "Method","BANK_TRANSFER","Direction","IN","Status","SUCCEEDED",
                "ProviderReference","BANK-W22-001")));
        when(db.queryForObject(startsWith("SELECT COUNT(*) FROM dbo.Payments WHERE PaymentID=? AND OrderID=?"),
            eq(Integer.class),eq(42L),eq(8L))).thenReturn(1);
    }

    @Test void identicalReplayReturnsOriginalPaymentWithoutNewInsert() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        previouslyPaid(db);
        long id=service(db).payment(8L,new BigDecimal("200000.00"),"BANK_TRANSFER","BANK-W22-001",KEY);
        assertEquals(42L,id);
        verify(db,never()).update(startsWith("INSERT INTO dbo.Payments"),any(Object[].class));
    }

    @Test void changedAmountWithSameIdempotencyKeyIsRejected() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        previouslyPaid(db);
        assertThrows(SaleProblem.class,()->service(db).payment(8L,new BigDecimal("300000.00"),
            "BANK_TRANSFER","BANK-W22-001",KEY));
        verify(db,never()).update(startsWith("INSERT INTO dbo.Payments"),any(Object[].class));
    }
}

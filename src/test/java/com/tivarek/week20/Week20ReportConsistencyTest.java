package com.tivarek.week20;

import com.tivarek.week17.ReportRange;
import com.tivarek.week17.ReportService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Week20ReportConsistencyTest {
    @Test void completedSalesReportUsesCanonicalInvoiceView() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForObject(anyString(),eq(BigDecimal.class),any(),any()))
                .thenReturn(new BigDecimal("297.00"));

        var range=ReportRange.parse("2026-11-01","2026-11-02");
        assertEquals(new BigDecimal("297.00"),new ReportService(db).completedOrderValue(range));

        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        verify(db).queryForObject(sql.capture(),eq(BigDecimal.class),any(),any());
        assertTrue(sql.getValue().contains("dbo.vSaleLineAmounts"));
        assertFalse(sql.getValue().contains("l.UnitPrice-l.DiscountAmount"));
    }
}

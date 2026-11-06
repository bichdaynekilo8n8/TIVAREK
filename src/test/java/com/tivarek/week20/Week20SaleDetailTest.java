package com.tivarek.week20;

import com.tivarek.week13.StorefrontRepository;
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

class Week20SaleDetailTest {
    @Test void salesDetailMatchesSqlServerViewForLineWideDiscountAndTax() {
        var db=mock(JdbcTemplate.class);
        when(db.queryForList(anyString(),eq(7L))).thenAnswer(invocation -> {
            String sql=invocation.getArgument(0);
            if(sql.contains("JOIN dbo.OrderShipping"))
                return List.of(Map.of("ShippingFee",new BigDecimal("0.00")));
            if(sql.contains("LEFT JOIN dbo.BulkSaleLines"))
                return List.of(Map.of("UnitPrice",new BigDecimal("100.00"),
                        "DiscountAmount",new BigDecimal("30.00"),"TaxRate",new BigDecimal("10.00"),
                        "Quantity",3));
            return List.of();
        });
        var service=new SalesService(db,mock(StorefrontRepository.class),mock(AuditService.class),
                mock(WarrantyIssuanceService.class));

        assertEquals(new BigDecimal("297.00"),service.detail(7L).total());
    }
}

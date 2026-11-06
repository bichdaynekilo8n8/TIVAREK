package com.tivarek.week15;

import com.tivarek.week13.StorefrontProductView;
import com.tivarek.week13.StorefrontRepository;
import com.tivarek.week8.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SaleCartTest {
    @Test void cartAllowsOneSerialAndRemovesOnZero() {
        var session=new MockHttpSession();
        SaleCart.put(session,"DEVICE",42,1);
        assertEquals(1,SaleCart.get(session).get("DEVICE:42"));
        assertThrows(SaleProblem.class,()->SaleCart.put(session,"DEVICE",42,2));
        SaleCart.put(session,"DEVICE",42,0);
        assertTrue(SaleCart.get(session).isEmpty());
    }
    @Test void rejectsInvalidKindsAndQuantities() {
        var session=new MockHttpSession();
        assertThrows(SaleProblem.class,()->SaleCart.put(session,"OTHER",1,1));
        assertThrows(SaleProblem.class,()->SaleCart.put(session,"BULK",0,1));
        assertThrows(SaleProblem.class,()->SaleCart.put(session,"BULK",2,10001));
    }
    @Test void previewUsesAuthoritativeDatabasePriceNotClientPrice() {
        var repo=mock(StorefrontRepository.class);
        when(repo.detail("BULK",7)).thenReturn(new StorefrontProductView(
            "BULK",7,"P07","Cáp HDMI","Phụ kiện",null,null,new BigDecimal("25000.00"),null,null,9,1,null));
        var sales=new SalesService(mock(JdbcTemplate.class),repo,mock(AuditService.class),mock(com.tivarek.week16.WarrantyIssuanceService.class));
        var view=sales.preview(java.util.Map.of("BULK:7",3));
        assertEquals(new BigDecimal("75000.00"),view.total());
        assertEquals(3,view.lines().getFirst().quantity());
    }
}

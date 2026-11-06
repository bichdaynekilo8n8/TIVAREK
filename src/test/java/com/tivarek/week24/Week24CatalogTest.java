package com.tivarek.week24;
import com.tivarek.week13.*;
import com.tivarek.week11.DeviceImageStorage;
import com.tivarek.week8.AuditService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class Week24CatalogTest {
    @Test void detailUsesOnePublicationReadAndStillRejectsHiddenProducts() {
        var repo=mock(StorefrontRepository.class);
        var service=new StorefrontService(repo,mock(DeviceImageStorage.class),mock(AuditService.class));
        var item=new StorefrontProductView("BULK",1,"P1","Cáp HDMI","Phụ kiện",null,null,BigDecimal.TEN,null,null,1,1,null);
        when(repo.detail("BULK",1)).thenReturn(item);
        when(repo.productSpecifications(1)).thenReturn(List.of());when(repo.productImages(1)).thenReturn(List.of());
        assertSame(item,service.bulkDetail(1).item());
        verify(repo,times(1)).detail("BULK",1);verify(repo).productSpecifications(1);verify(repo).productImages(1);
        when(repo.detail("BULK",2)).thenThrow(com.tivarek.week10.CatalogProblem.missing());
        assertThrows(com.tivarek.week10.CatalogProblem.class,()->service.bulkDetail(2));
        verify(repo,never()).productSpecifications(2);verify(repo,never()).productImages(2);
    }
}

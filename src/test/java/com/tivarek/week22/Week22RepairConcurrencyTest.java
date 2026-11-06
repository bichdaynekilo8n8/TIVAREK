package com.tivarek.week22;

import com.tivarek.week12.InventoryService;
import com.tivarek.week14.RepairProblem;
import com.tivarek.week14.RepairService;
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

class Week22RepairConcurrencyTest {
    private static RepairService service(JdbcTemplate db) {
        return new RepairService(db,mock(AuditService.class),mock(InventoryService.class),
            mock(WarrantyIssuanceService.class));
    }

    @Test void staleHandoverIsRejectedUnderTicketUpdateLock() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        when(db.queryForList(contains("RepairTickets WITH (UPDLOCK,HOLDLOCK)"),eq(41L)))
            .thenReturn(List.of(Map.of("RepairTicketID",41L,"Status","DELIVERED")));
        assertThrows(RepairProblem.class,()->service(db).handover(41L,BigDecimal.ZERO,"CASH","Biên nhận"));
        verify(db).queryForList(contains("RepairTickets WITH (UPDLOCK,HOLDLOCK)"),eq(41L));
        verify(db,never()).update(startsWith("UPDATE dbo.RepairTickets SET Status='DELIVERED'"),anyLong());
    }

    @Test void invalidTicketTransitionCannotWriteEvent() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        when(db.queryForList(contains("RepairTickets WITH (UPDLOCK,HOLDLOCK)"),eq(41L)))
            .thenReturn(List.of(Map.of("RepairTicketID",41L,"Status","DELIVERED")));
        assertThrows(RepairProblem.class,()->service(db).status(41L,"REPAIRING","Không hợp lệ",true));
        verify(db,never()).update(startsWith("INSERT INTO dbo.RepairEvents"),any(Object[].class));
    }

    @Test void duplicateIntakeChecksTheRequestUnderUpdateLock() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        when(db.queryForList(contains("RepairRequests WITH (UPDLOCK,HOLDLOCK)"),eq(9L)))
            .thenReturn(List.of(Map.of("Status","ACCEPTED")));
        when(db.queryForObject(startsWith("SELECT COUNT(*) FROM dbo.RepairTickets WHERE RequestID=?"),
            eq(Integer.class),eq(9L))).thenReturn(1);
        assertThrows(RepairProblem.class,()->service(db).intake(9L,null,"Người dùng","0335135549",
            "Tivi","SERIAL","Hỏng nguồn","Có vết trầy","",null));
        verify(db).queryForList(contains("RepairRequests WITH (UPDLOCK,HOLDLOCK)"),eq(9L));
    }
}

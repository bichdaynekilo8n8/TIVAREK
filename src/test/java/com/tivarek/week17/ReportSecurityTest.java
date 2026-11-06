package com.tivarek.week17;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportSecurityTest {
    @Autowired MockMvc mvc;

    @Test void guestCannotSeeReportOrExport() throws Exception {
        mvc.perform(get("/admin/reports")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/reports/export/finance")).andExpect(status().is3xxRedirection());
    }

    @Test void stockEmployeeCannotReadFinancialReport() throws Exception {
        var stock=user("inventory").authorities(new SimpleGrantedAuthority("REPORT_STOCK"));
        mvc.perform(get("/admin/reports/finance").with(stock)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports/export/finance").with(stock)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports").with(stock)).andExpect(status().isForbidden());
    }

    @Test void unregisteredExportRouteIsDenied() throws Exception {
        mvc.perform(get("/admin/reports/export/secret").with(user("owner").authorities(
            new SimpleGrantedAuthority("REPORT_OVERVIEW")))).andExpect(status().isForbidden());
    }
}

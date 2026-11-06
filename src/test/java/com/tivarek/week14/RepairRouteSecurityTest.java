package com.tivarek.week14;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RepairRouteSecurityTest {
    @Autowired MockMvc mvc;

    @Test void publicRepairPagesAccessibleWithoutLogin() throws Exception {
        mvc.perform(get("/sua-chua")).andExpect(status().isOk()).andExpect(view().name("repair-request"));
        mvc.perform(get("/tra-cuu-sua-chua")).andExpect(status().isOk()).andExpect(view().name("repair-tracking"));
    }
    @Test void adminStockAndRepairsRequireAuthentication() throws Exception {
        mvc.perform(get("/admin/stock")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/repairs")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
}

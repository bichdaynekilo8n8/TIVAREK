package com.tivarek.week16;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WarrantyRoutesTest {
    @Autowired MockMvc mvc;

    @Test void publicLookupIsAvailableButPrivatePagesAreProtected() throws Exception {
        mvc.perform(get("/tra-cuu-bao-hanh")).andExpect(status().isOk())
            .andExpect(view().name("warranty/lookup"));
        mvc.perform(get("/admin/warranties")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/account/warranties")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test void publicPostStillRequiresCsrf() throws Exception {
        mvc.perform(post("/tra-cuu-bao-hanh").param("code","BH-AAAAAAAAAAAAAAAAAAAAAAAA")
            .param("token","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/bao-hanh/yeu-cau").param("code","BH-AAAAAAAAAAAAAAAAAAAAAAAA")
            .param("token","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
            .param("quantity","1").param("problem","Không lên nguồn"))
            .andExpect(status().isForbidden());
    }

    @Test void malformedLookupIsRejectedBeforeDatabaseAccess() {
        var service=new WarrantyService(org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class),
            org.mockito.Mockito.mock(com.tivarek.week8.AuditService.class));
        org.junit.jupiter.api.Assertions.assertThrows(WarrantyProblem.class,()->service.lookup("BH-1","short"));
    }
}

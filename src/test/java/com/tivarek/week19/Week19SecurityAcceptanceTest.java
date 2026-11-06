package com.tivarek.week19;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cross-module, negative-path acceptance tests; uses H2 and never touches production. */
@SpringBootTest(classes = TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Week19SecurityAcceptanceTest {
    @Autowired MockMvc mvc;

    @Test void publicSalesRepairWarrantyPagesStayAvailable() throws Exception {
        for (String page : new String[]{"/san-pham", "/gio-hang", "/thanh-toan",
                "/sua-chua", "/tra-cuu-sua-chua", "/tra-cuu-bao-hanh"}) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
    }

    @Test void guestsCannotReadCustomerOrBackOfficeData() throws Exception {
        for (String page : new String[]{"/admin/stock", "/admin/orders", "/admin/repairs",
                "/admin/warranties", "/admin/reports", "/account/orders",
                "/account/repairs", "/account/warranties"}) {
            mvc.perform(get(page)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
        mvc.perform(get("/api/account/customer")).andExpect(status().isUnauthorized());
    }

    @Test void publicWritesStillRequireCsrf() throws Exception {
        for (String action : new String[]{"/gio-hang/add", "/thanh-toan", "/sua-chua",
                "/tra-cuu-sua-chua", "/tra-cuu-bao-hanh", "/bao-hanh/yeu-cau"}) {
            mvc.perform(post(action)).andExpect(status().isForbidden());
        }
    }

    @Test void inventoryOnlyUserCannotReadOtherDepartments() throws Exception {
        var inventory = user("week19-stock").authorities(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("STOCK_READ"));
        mvc.perform(get("/admin/orders").with(inventory)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/repairs").with(inventory)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/warranties").with(inventory)).andExpect(status().isForbidden());
        mvc.perform(get("/admin/reports/finance").with(inventory)).andExpect(status().isForbidden());
    }
}

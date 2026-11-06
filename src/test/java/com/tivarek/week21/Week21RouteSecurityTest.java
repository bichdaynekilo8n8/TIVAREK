package com.tivarek.week21;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Denied requests must be rejected BEFORE a controller or database mutation. */
@SpringBootTest(classes = TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Week21RouteSecurityTest {
    @Autowired MockMvc mvc;

    @Test void readOnlySalesRoleCannotMutateOrders() throws Exception {
        var salesReader = user("reader").authorities(() -> "SALE_READ");
        for (String endpoint : new String[]{"/admin/orders/1/confirm", "/admin/orders/1/payment",
                "/admin/orders/1/refund", "/admin/orders/1/return", "/admin/orders/1/ship",
                "/admin/orders/1/lines/add"}) {
            mvc.perform(post(endpoint).with(salesReader).with(csrf()))
                .andExpect(status().isForbidden());
        }
    }

    @Test void readOnlyWarrantyRoleCannotManagePoliciesOrClaims() throws Exception {
        var reader = user("reader").authorities(() -> "WARRANTY_READ");
        for (String endpoint : new String[]{"/admin/warranties/policies", "/admin/warranties/settings",
                "/admin/warranties/1/token", "/admin/warranties/claims/1/status",
                "/admin/warranties/claims/1/repair"}) {
            mvc.perform(post(endpoint).with(reader).with(csrf()))
                .andExpect(status().isForbidden());
        }
    }

    @Test void readOnlyStockAndRepairRolesCannotMutate() throws Exception {
        var stockReader = user("reader").authorities(() -> "STOCK_READ");
        mvc.perform(post("/admin/stock/1/reverse").with(stockReader).with(csrf()))
            .andExpect(status().isForbidden());
        mvc.perform(post("/admin/stock/draft").with(stockReader).with(csrf()))
            .andExpect(status().isForbidden());
        var repairReader = user("reader").authorities(() -> "REPAIR_READ");
        for (String endpoint : new String[]{"/admin/repairs/intake", "/admin/repairs/1/assign",
                "/admin/repairs/1/quotes", "/admin/repairs/1/work", "/admin/repairs/1/handover"}) {
            mvc.perform(post(endpoint).with(repairReader).with(csrf()))
                .andExpect(status().isForbidden());
        }
    }

    @Test void passwordPageRequiresLoginAndAllWritesRequireCsrf() throws Exception {
        mvc.perform(get("/doi-mat-khau")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/doi-mat-khau").with(user("owner"))).andExpect(status().isOk());
        mvc.perform(post("/doi-mat-khau").with(user("owner"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/orders/1/refund").with(user("manager").authorities(() -> "SALE_REFUND")))
            .andExpect(status().isForbidden());
    }

    @Test void trackingTokenResponsesDenyReferrerLeakage() throws Exception {
        mvc.perform(get("/login")).andExpect(header().string("Referrer-Policy", "no-referrer"));
    }
}

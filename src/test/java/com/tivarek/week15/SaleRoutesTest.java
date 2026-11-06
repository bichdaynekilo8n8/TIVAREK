package com.tivarek.week15;

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
class SaleRoutesTest {
    @Autowired MockMvc mvc;

    @Test void anonymousMaySeeCartAndCheckoutButNotStaffOrPrivateOrders() throws Exception {
        mvc.perform(get("/gio-hang")).andExpect(status().isOk()).andExpect(view().name("week15/cart"));
        mvc.perform(get("/thanh-toan")).andExpect(status().isOk()).andExpect(view().name("week15/checkout"));
        mvc.perform(get("/admin/orders")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/account/orders")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test void cartMutationRequiresCsrfToken() throws Exception {
        mvc.perform(post("/gio-hang/add").param("kind","BULK").param("id","1").param("quantity","1"))
            .andExpect(status().isForbidden());
    }
}

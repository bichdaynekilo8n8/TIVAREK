package com.tivarek.week8;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.tivarek.config.SecurityConfig;
import com.tivarek.controller.HomeController;
import com.tivarek.controller.AuthPageController;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CustomerController.class,Week9ApiController.class,PageController.class,HomeController.class,AuthPageController.class})
@Import(SecurityConfig.class)
class RouteAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean com.tivarek.service.ProductService productService;
    @MockitoBean com.tivarek.week13.StorefrontService storefrontService;
    @MockitoBean CustomerService customers;
    @MockitoBean CatalogService catalog;
    @MockitoBean AccountService accounts;
    @MockitoBean AuditQueryService auditQuery;
    @MockitoBean AuditService audit;
    @MockitoBean UserDetailsService users;
    @MockitoBean com.tivarek.auth.LoginAttemptGuard loginAttempts;

    @Test void anonymousApiGets401() throws Exception {
        mvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
        verifyNoInteractions(customers);
    }
    @Test void anonymousAdminRedirectsToLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }
    @Test void salesCannotSeeCostsEvenWhenEnteringUrlDirectly() throws Exception {
        mvc.perform(get("/api/admin/costs").with(user("sales").authorities(
            new SimpleGrantedAuthority("ADMIN_ACCESS"),new SimpleGrantedAuthority("CUSTOMER_READ"))))
            .andExpect(status().isForbidden());
        verifyNoInteractions(catalog);
    }
    @Test void costPermissionAllowsRequest() throws Exception {
        when(catalog.costs()).thenReturn(List.of(new CatalogService.CostView("BULK",1,"MOUSE","Mouse",new BigDecimal("50000"),"PN1")));
        mvc.perform(get("/api/admin/costs").with(user("owner").authorities(new SimpleGrantedAuthority("COST_READ"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].unitCost").value(50000));
    }
    @Test void technicianCannotReadCustomerDirectoryOnBothRoutes() throws Exception {
        for (String path:List.of("/api/admin/customers","/api/week8/customers","/api/week8/customers/1"))
            mvc.perform(get(path).with(user("tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(customers);
    }
    @Test void publicProductsHaveNoCostField() throws Exception {
        when(catalog.products()).thenReturn(List.of(new CatalogService.ProductView(1,"MOUSE","Mouse",new BigDecimal("90000"))));
        mvc.perform(get("/api/public/products")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].listPrice").value(90000))
            .andExpect(jsonPath("$[0].unitCost").doesNotExist())
            .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }
    @Test void writeRequiresCsrfEvenWithPermission() throws Exception {
        mvc.perform(post("/api/admin/customers").with(user("sales").authorities(new SimpleGrantedAuthority("CUSTOMER_WRITE")))
            .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(customers);
    }
    @Test void csrfDoesNotGrantWritePermission() throws Exception {
        mvc.perform(post("/api/admin/customers").with(user("tech").authorities(new SimpleGrantedAuthority("ADMIN_ACCESS")))
            .with(csrf()).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(customers);
    }
    @Test void adminTemplateRendersActualServiceValues() throws Exception {
        when(customers.search("")).thenReturn(List.of(new Customer(5L,"KH5","Khach A","0000000000",null,java.time.Instant.now())));
        when(catalog.costs()).thenReturn(List.of(new CatalogService.CostView("BULK",1,"MOUSE","Mouse",new BigDecimal("50000"),"PN1")));
        when(auditQuery.recent()).thenReturn(List.of(new AuditQueryService.Entry(1,"owner","LOGIN_SUCCESS","USER","owner","Login",java.time.LocalDateTime.now())));
        mvc.perform(get("/admin").with(user("owner").authorities(
            new SimpleGrantedAuthority("ADMIN_ACCESS"),new SimpleGrantedAuthority("CUSTOMER_READ"),
            new SimpleGrantedAuthority("COST_READ"),new SimpleGrantedAuthority("AUDIT_READ"))))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("KH5")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("50.000")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("LOGIN_SUCCESS")));
    }
    @Test void accountTemplateRendersOwnProfile() throws Exception {
        when(accounts.mine("alice",null)).thenReturn(new AccountService.Profile(9,"KH9","Alice","0000000000",null));
        mvc.perform(get("/account").with(user("alice").authorities(new SimpleGrantedAuthority("CUSTOMER_SELF_READ"))))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Alice")));
    }
    @Test void publicTemplateRendersProducts() throws Exception {
        when(productService.getFeaturedProducts()).thenReturn(List.of(new com.tivarek.dto.ProductCardDto(1L,"Mouse",new BigDecimal("90000"))));
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("90.000")));
    }
    @Test void customerCannotOpenAdmin() throws Exception {
        mvc.perform(get("/admin").with(user("customer").authorities(new SimpleGrantedAuthority("CUSTOMER_SELF_READ"))))
            .andExpect(status().isForbidden());
    }
}

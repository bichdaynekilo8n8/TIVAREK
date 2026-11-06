package com.tivarek.week8;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(MethodAuthorizationTest.Config.class)
class MethodAuthorizationTest {
    @Configuration @EnableMethodSecurity @org.springframework.context.annotation.Import(ProductConfig.class)
    static class Config {
        @Bean JdbcTemplate jdbc() { return mock(JdbcTemplate.class); }
        @Bean CatalogService catalog(JdbcTemplate j) { return new CatalogService(j); }
        @Bean AccountService accounts(JdbcTemplate j) { return new AccountService(j); }
    }
    @org.springframework.context.annotation.Import({com.tivarek.week10.ProductManagementService.class,
        com.tivarek.week10.LookupService.class,com.tivarek.week10.ManagedProductRepository.class,
        com.tivarek.week10.LookupRepository.class,AuditService.class,com.tivarek.week11.DeviceService.class,com.tivarek.week11.DeviceRepository.class})
    @Configuration static class ProductConfig {}
    @Autowired com.tivarek.week10.ProductManagementService products;
    @Autowired com.tivarek.week10.LookupService lookups;
    @Autowired com.tivarek.week11.DeviceService devices;
    @Autowired CatalogService catalog;
    @Autowired AccountService accounts;
    @Autowired JdbcTemplate jdbc;

    @org.junit.jupiter.api.BeforeEach
    void clearLifecycleCalls() { clearInvocations(jdbc); }

    @Test @WithMockUser(authorities="CUSTOMER_READ")
    void callingServiceDirectlyStillCannotReadCosts() {
        assertThrows(AccessDeniedException.class,() -> catalog.costs());
        verifyNoInteractions(jdbc);
    }
    @Test @WithMockUser(authorities="PRODUCT_READ")
    void readPermissionCannotInvokeProductOrCatalogWritesDirectly() {
        assertThrows(AccessDeniedException.class,() -> products.create(com.tivarek.week10.ProductInput.empty()));
        assertThrows(AccessDeniedException.class,() -> lookups.create(com.tivarek.week10.CatalogKind.categories,com.tivarek.week10.LookupInput.empty()));
        verifyNoInteractions(jdbc);
    }
    @Test @WithMockUser(authorities="DEVICE_READ")
    void deviceReadCannotWriteMetadataOrInspectionsThroughService() {
        assertThrows(AccessDeniedException.class,() -> devices.create(com.tivarek.week11.DeviceInput.empty()));
        assertThrows(AccessDeniedException.class,() -> devices.addCheck(1,new com.tivarek.week11.CheckInput("X","PASS","0000000000000000")));
        verifyNoInteractions(jdbc);
    }
    @Test @WithMockUser(username="alice",authorities="CUSTOMER_SELF_READ")
    void cannotInvokeOwnProfileServiceAsAnotherUsername() {
        assertThrows(AccessDeniedException.class,() -> accounts.mine("bob",null));
        verifyNoInteractions(jdbc);
    }
}

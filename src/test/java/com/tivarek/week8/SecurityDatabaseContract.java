package com.tivarek.week8;

import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class SecurityDatabaseContract {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CustomerService customerService;
    String prefix;
    long ownId,otherId;
    static final String PASSWORD="Integration-Only-Password-9";
    static final String HASH=new BCryptPasswordEncoder(10).encode(PASSWORD);
    abstract void verifyDatabase();

    @BeforeEach void fixture() {
        verifyDatabase();
        prefix="it."+UUID.randomUUID().toString().substring(0,8)+".";
        for(String role:new String[]{"OWNER","SALES","TECHNICIAN","INVENTORY","CUSTOMER","OTHER"}) {
            jdbc.update("INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName) VALUES (?,?,N'Integration Test')",prefix+role,HASH);
            Long id=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,prefix+role);
            jdbc.update("INSERT INTO dbo.UserRoles(UserID,RoleID) SELECT ?,RoleID FROM dbo.Roles WHERE RoleCode=?",id,role.equals("OTHER")?"CUSTOMER":role);
            if(role.equals("CUSTOMER")||role.equals("OTHER")) {
                jdbc.update("INSERT INTO dbo.Customers(CustomerCode,FullName,Phone,UserID) VALUES (?,N'Khách kiểm thử','0000000000',?)",prefix+role,id);
                long c=jdbc.queryForObject("SELECT CustomerID FROM dbo.Customers WHERE CustomerCode=?",Long.class,prefix+role);
                if(role.equals("CUSTOMER")) ownId=c; else otherId=c;
            }
        }
    }
    @AfterEach void cleanCommittedFixtures() {
        if (prefix==null || org.springframework.test.context.transaction.TestTransaction.isActive()) return;
        String like=prefix+"%";
        jdbc.update("DELETE FROM dbo.AuditLogs WHERE ActorID IN (SELECT UserID FROM dbo.Users WHERE LoginName LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.CustomerAddresses WHERE CustomerID IN (SELECT CustomerID FROM dbo.Customers WHERE UserID IN (SELECT UserID FROM dbo.Users WHERE LoginName LIKE ?))",like);
        jdbc.update("DELETE FROM dbo.Customers WHERE UserID IN (SELECT UserID FROM dbo.Users WHERE LoginName LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.UserRoles WHERE UserID IN (SELECT UserID FROM dbo.Users WHERE LoginName LIKE ?)",like);
        jdbc.update("DELETE FROM dbo.Users WHERE LoginName LIKE ?",like);
    }
    MockHttpSession login(String role) throws Exception {
        var result=mvc.perform(formLogin().user(prefix+role).password(PASSWORD))
            .andExpect(authenticated().withUsername(prefix+role)).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    @ParameterizedTest
    @CsvSource({"OWNER,200,200,200","SALES,403,200,200","TECHNICIAN,403,403,200","INVENTORY,200,403,200","CUSTOMER,403,403,403"})
    void databaseRoleMatrix(String role,int cost,int customers,int admin) throws Exception {
        var session=login(role);
        mvc.perform(get("/api/admin/costs").session(session)).andExpect(status().is(cost));
        mvc.perform(get("/api/admin/customers").session(session)).andExpect(status().is(customers));
        mvc.perform(get("/api/week8/customers").session(session)).andExpect(status().is(customers));
        mvc.perform(get("/admin").session(session)).andExpect(status().is(admin));
    }
    @Test void customerCannotGuessAnotherCustomersId() throws Exception {
        var session=login("CUSTOMER");
        mvc.perform(get("/api/account/customers/"+ownId).session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.customerId").value(ownId));
        mvc.perform(get("/api/account/customers/"+otherId).session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/api/account/customer").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.customerId").value(ownId));
    }
    @Test void wrongPasswordAndDisabledAccountAreRejected() throws Exception {
        mvc.perform(formLogin().user(prefix+"OWNER").password("wrong")).andExpect(unauthenticated());
        jdbc.update("UPDATE dbo.Users SET IsActive=0 WHERE LoginName=?",prefix+"OWNER");
        mvc.perform(formLogin().user(prefix+"OWNER").password(PASSWORD)).andExpect(unauthenticated());
    }
    @Test void veryLongPasswordIsRejectedAndFailedLoginIsAudited() throws Exception {
        mvc.perform(formLogin().user(prefix+"OWNER").password("x".repeat(1000))).andExpect(unauthenticated());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AuditLogs a JOIN dbo.Users u ON u.UserID=a.ActorID WHERE u.LoginName=? AND a.Action='LOGIN_FAILURE'",Integer.class,prefix+"OWNER"));
    }
    @Test void logoutInvalidatesSessionAndWritesAudit() throws Exception {
        var session=login("OWNER");
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());
        assertTrue(session.isInvalid());
        assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AuditLogs a JOIN dbo.Users u ON u.UserID=a.ActorID WHERE u.LoginName=? AND a.Action='LOGOUT'",Long.class,prefix+"OWNER"));
        mvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
    }
    @Test void customerUpdateWritesCorrectActor() throws Exception {
        var session=login("SALES");
        mvc.perform(put("/api/admin/customers/"+ownId).session(session).with(csrf()).contentType("application/json")
            .content("{\"fullName\":\"Khách cập nhật\",\"phone\":\"0000000000\",\"email\":null}"))
            .andExpect(status().isOk());
        assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AuditLogs a JOIN dbo.Users u ON u.UserID=a.ActorID WHERE u.LoginName=? AND a.Action='CUSTOMER_UPDATE' AND a.EntityKey=?",Long.class,prefix+"SALES",Long.toString(ownId)));
    }
    @Test void customerCreationPersistsDataAndAudit() throws Exception {
        var session=login("SALES"); String code="W9-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(java.util.Locale.ROOT);
        mvc.perform(post("/api/admin/customers").session(session).with(csrf()).contentType("application/json")
            .content("{\"customerCode\":\""+code+"\",\"fullName\":\"Khách tạo mới\",\"phone\":\"0000000000\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.customerCode").value(code));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Customers WHERE CustomerCode=?",Integer.class,code));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.AuditLogs a JOIN dbo.Users u ON u.UserID=a.ActorID WHERE u.LoginName=? AND Action='CUSTOMER_CREATE'",Integer.class,prefix+"SALES"));
    }
    @Test void invalidCreateDoesNotWriteCustomer() throws Exception {
        var session=login("SALES");
        mvc.perform(post("/api/admin/customers").session(session).with(csrf()).contentType("application/json").content("{\"customerCode\":\"BAD\",\"fullName\":\"\",\"phone\":\"bad\"}"))
            .andExpect(status().isBadRequest());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Customers WHERE CustomerCode='BAD'",Integer.class));
    }
    @Test
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void databaseAuditFailureRollsBackCustomerChange() throws Exception {
        var session=login("SALES");
        String before=jdbc.queryForObject("SELECT FullName FROM dbo.Customers WHERE CustomerID=?",String.class,ownId);
        // Bỏ Actor trong DB sau khi login: audit phải thất bại và UPDATE phải rollback.
        Long actor=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,prefix+"SALES");
        jdbc.update("UPDATE dbo.Users SET LoginName=? WHERE UserID=?",prefix+"RENAMED",actor);
        assertThrows(Exception.class,() -> mvc.perform(put("/api/admin/customers/"+ownId).session(session).with(csrf()).contentType("application/json")
            .content("{\"fullName\":\"Must rollback\",\"phone\":\"0000000000\"}")));
        assertEquals(before,jdbc.queryForObject("SELECT FullName FROM dbo.Customers WHERE CustomerID=?",String.class,ownId));
    }
    @Test void customerFormCreateAndEditWork() throws Exception {
        var session=login("SALES"); String code="FORM-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(java.util.Locale.ROOT);
        mvc.perform(post("/admin/customers").session(session).with(csrf()).param("customerCode",code).param("fullName","Khách từ form").param("phone","0000000000"))
            .andExpect(status().is3xxRedirection());
        long id=jdbc.queryForObject("SELECT CustomerID FROM dbo.Customers WHERE CustomerCode=?",Long.class,code);
        mvc.perform(get("/admin").session(session).param("editId",Long.toString(id))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Khách từ form")));
        mvc.perform(post("/admin/customers/"+id).session(session).with(csrf()).param("fullName","Khách đã sửa").param("phone","0000000000"))
            .andExpect(status().is3xxRedirection());
        assertEquals("Khách đã sửa",jdbc.queryForObject("SELECT FullName FROM dbo.Customers WHERE CustomerID=?",String.class,id));
    }
    @Test void inventoryCannotSubmitCustomerForm() throws Exception {
        mvc.perform(post("/admin/customers").session(login("INVENTORY")).with(csrf()).param("customerCode","BLOCKED").param("fullName","Blocked").param("phone","0000000000"))
            .andExpect(status().isForbidden());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Customers WHERE CustomerCode='BLOCKED'",Integer.class));
    }
    @Test void actualPublicResponsesAndHomepageContainNoCostOrPassword() throws Exception {
        mvc.perform(get("/api/public/products")).andExpect(status().isOk()).andExpect(jsonPath("$[0].listPrice").exists()).andExpect(jsonPath("$[0].unitCost").doesNotExist()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("TIVAREK")));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
    }
    @Test void ownerGetsBothDeviceAndBulkReceiptCosts() throws Exception {
        mvc.perform(get("/api/admin/costs").session(login("OWNER"))).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.stockType == 'BULK')]").isNotEmpty()).andExpect(jsonPath("$[?(@.stockType == 'DEVICE')]").isNotEmpty());
    }
}

package com.tivarek.auth;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder encoder;

    @Test void registrationCreatesDatabaseCustomerOnlyAndBCrypt() throws Exception {
        mvc.perform(post("/register").with(csrf())
            .param("username","new.tivarek.customer")
            .param("fullName","Nguyễn Văn Test")
            .param("phone","0912345678")
            .param("email","new@example.com")
            .param("password","Secure1234567!")
            .param("confirmPassword","Secure1234567!"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl("/login?registered"));
        var record=db.queryForMap("""
          SELECT u.UserID,u.PasswordHash,c.CustomerID FROM dbo.Users u
          JOIN dbo.Customers c ON c.UserID=u.UserID WHERE u.LoginName='new.tivarek.customer'
          """);
        assertTrue(encoder.matches("Secure1234567!",(String)record.get("PasswordHash")));
        assertNotEquals("Secure1234567!",record.get("PasswordHash"));
        var roles=db.queryForList("""
          SELECT r.RoleCode FROM dbo.UserRoles ur JOIN dbo.Roles r ON r.RoleID=ur.RoleID
          WHERE ur.UserID=?
          """,String.class,record.get("UserID"));
        assertEquals(java.util.List.of("CUSTOMER"),roles);
    }

    @Test void registrationRejectsNoCsrfAndBadPasswords() throws Exception {
        mvc.perform(post("/register").param("username","withoutcsrf")
            .param("fullName","Example").param("phone","0912345678")
            .param("password","Secure1234567!").param("confirmPassword","Secure1234567!"))
          .andExpect(status().isForbidden());
        mvc.perform(post("/register").with(csrf()).param("username","bad.password")
            .param("fullName","Example").param("phone","0912345678")
            .param("password","short").param("confirmPassword","different"))
          .andExpect(status().isOk()).andExpect(view().name("register"));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM dbo.Users WHERE LoginName='bad.password'",Integer.class));
    }

    @Test void missingCustomerRoleShowsFormErrorWithoutOrphanUser() throws Exception {
        db.update("DELETE FROM dbo.RolePermissions WHERE RoleID IN (SELECT RoleID FROM dbo.Roles WHERE RoleCode='CUSTOMER')");
        db.update("DELETE FROM dbo.UserRoles WHERE RoleID IN (SELECT RoleID FROM dbo.Roles WHERE RoleCode='CUSTOMER')");
        db.update("DELETE FROM dbo.Roles WHERE RoleCode='CUSTOMER'");
        mvc.perform(post("/register").with(csrf())
            .param("username","missing.role.account")
            .param("fullName","Test Missing Role")
            .param("phone","0912345678")
            .param("password","Secure1234567!")
            .param("confirmPassword","Secure1234567!"))
          .andExpect(status().isOk())
          .andExpect(view().name("register"))
          .andExpect(model().attributeExists("failure"));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM dbo.Users WHERE LoginName='missing.role.account'",Integer.class));
    }
}

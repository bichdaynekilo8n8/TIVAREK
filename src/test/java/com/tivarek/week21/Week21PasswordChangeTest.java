package com.tivarek.week21;

import com.tivarek.TivarekApplication;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TivarekApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Week21PasswordChangeTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder passwords;

    @Test void wrongCurrentPasswordNeverChangesStoredHash() throws Exception {
        String username = seed();
        String oldHash = hash(username);
        mvc.perform(post("/doi-mat-khau").with(user(username)).with(csrf())
            .param("currentPassword", "wrong-password")
            .param("newPassword", "A-different-password-123")
            .param("confirmPassword", "A-different-password-123"))
            .andExpect(status().isOk()).andExpect(view().name("auth/change-password"));
        assertEquals(oldHash, hash(username));
    }

    @Test void correctChangePersistsBcryptAndCreatesAuditWithoutSecrets() throws Exception {
        String username = seed();
        mvc.perform(post("/doi-mat-khau").with(user(username)).with(csrf())
            .param("currentPassword", "Current-password-123")
            .param("newPassword", "A-different-password-123")
            .param("confirmPassword", "A-different-password-123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?passwordChanged"));
        assertTrue(passwords.matches("A-different-password-123", hash(username)));
        assertFalse(passwords.matches("Current-password-123", hash(username)));
        String summary = db.queryForObject("""
            SELECT TOP 1 Summary FROM dbo.AuditLogs WHERE ActorID =
            (SELECT UserID FROM dbo.Users WHERE LoginName=?) AND Action='PASSWORD_CHANGE'
            ORDER BY AuditID DESC
            """, String.class, username);
        assertNotNull(summary);
        assertFalse(summary.contains("A-different-password-123"));
    }

    @Test void forbiddenFinancialActionIsAuditedWithoutRequestIdentifiers() throws Exception {
        String username = seed();
        mvc.perform(post("/admin/orders/987654/refund").with(user(username).authorities(() -> "SALE_READ"))
            .with(csrf())).andExpect(status().isForbidden());
        String summary = db.queryForObject("""
            SELECT TOP 1 Summary FROM dbo.AuditLogs WHERE ActorID=
            (SELECT UserID FROM dbo.Users WHERE LoginName=?) AND Action='ACCESS_DENIED'
            ORDER BY AuditID DESC
            """, String.class, username);
        assertNotNull(summary);
        assertTrue(summary.contains("SALES"));
        assertFalse(summary.contains("987654"));
    }

    private String seed() {
        String username = "w21." + UUID.randomUUID().toString().substring(0, 15);
        db.update("INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName) VALUES (?,?,N'Week 21 security test')",
            username, passwords.encode("Current-password-123"));
        return username;
    }

    private String hash(String username) {
        return db.queryForObject("SELECT PasswordHash FROM dbo.Users WHERE LoginName=?", String.class, username);
    }
}

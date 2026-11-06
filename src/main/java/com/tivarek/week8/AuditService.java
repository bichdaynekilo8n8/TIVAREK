package com.tivarek.week8;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Transactional
    public void writeCurrent(String action,String type,String key,String summary) {
        writeFor(SecurityContextHolder.getContext().getAuthentication(),action,type,key,summary);
    }

    @Transactional
    public void writeFor(Authentication auth,String action,String type,String key,String summary) {
        if (auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new IllegalStateException("Nhật ký thao tác cần người dùng đã đăng nhập");
        Long actor=jdbc.queryForObject("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,auth.getName());
        jdbc.update("""
            INSERT INTO dbo.AuditLogs(ActorID,Action,EntityType,EntityKey,Summary) VALUES (?,?,?,?,?)
            """,actor,action,type,key,summary);
    }
    @Transactional
    public void failedLogin(String username) {
        var actors=(username==null || username.length()>100) ? java.util.List.<Long>of() :
            jdbc.queryForList("SELECT UserID FROM dbo.Users WHERE LoginName=?",Long.class,username);
        Long actor=actors.isEmpty() ? null : actors.getFirst();
        jdbc.update("INSERT INTO dbo.AuditLogs(ActorID,Action,EntityType,EntityKey,Summary) VALUES (?,'LOGIN_FAILURE','AUTH','LOGIN',N'Đăng nhập không thành công')",actor);
    }

    /** Log only a fixed route CATEGORY, never a URL with IDs, tokens or query parameters. */
    @Transactional
    public void deniedRequest(Authentication authentication, String method, String path) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) return;
        var actors = jdbc.queryForList("SELECT UserID FROM dbo.Users WHERE LoginName=?", Long.class,
            authentication.getName());
        if (actors.isEmpty()) return;
        String category = path == null ? "OTHER" :
            path.startsWith("/admin/orders") ? "SALES" :
            path.startsWith("/admin/stock") ? "STOCK" :
            path.startsWith("/admin/repairs") ? "REPAIR" :
            path.startsWith("/admin/warranties") ? "WARRANTY" :
            path.startsWith("/admin/reports") ? "REPORT" :
            path.startsWith("/admin") ? "ADMIN" :
            path.startsWith("/account") || path.startsWith("/doi-mat-khau") ? "ACCOUNT" : "OTHER";
        String verb = java.util.Set.of("GET", "POST", "PUT", "PATCH", "DELETE")
            .contains(method) ? method : "OTHER";
        jdbc.update("""
            INSERT INTO dbo.AuditLogs(ActorID,Action,EntityType,EntityKey,Summary)
            VALUES (?,'ACCESS_DENIED','AUTH',?,?)
            """, actors.getFirst(), category, verb + " bị từ chối: " + category);
    }
}

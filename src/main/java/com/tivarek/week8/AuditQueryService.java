package com.tivarek.week8;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {
    private final JdbcTemplate jdbc;
    public AuditQueryService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Transactional(readOnly=true)
    public List<Entry> recent() {
        return jdbc.query("""
            SELECT TOP (100) a.AuditID,u.LoginName,a.Action,a.EntityType,a.EntityKey,a.Summary,a.OccurredAt
            FROM dbo.AuditLogs a LEFT JOIN dbo.Users u ON u.UserID=a.ActorID
            ORDER BY a.AuditID DESC
            """,(rs,n) -> new Entry(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),
                                   rs.getString(5),rs.getString(6),rs.getTimestamp(7).toLocalDateTime()));
    }
    public record Entry(long auditId,String loginName,String action,String entityType,
                         String entityKey,String summary,LocalDateTime occurredAt) {}
}

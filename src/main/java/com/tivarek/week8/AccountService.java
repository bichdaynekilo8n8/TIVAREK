package com.tivarek.week8;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final JdbcTemplate jdbc;
    public AccountService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ') and #username == authentication.name")
    @Transactional(readOnly=true)
    public Profile mine(String username,Long requestedId) {
        // UserID được xác định từ phiên đăng nhập. Không tin CustomerID do client gửi.
        var rows=jdbc.query("""
            SELECT c.CustomerID,c.CustomerCode,c.FullName,c.Phone,c.Email
            FROM dbo.Customers c JOIN dbo.Users u ON u.UserID=c.UserID
            WHERE u.LoginName=? """ + (requestedId==null ? "" : " AND c.CustomerID=?"),(rs,n) -> new Profile(rs.getLong(1),rs.getString(2),rs.getString(3),
                                      rs.getString(4),rs.getString(5)),requestedId==null ? new Object[]{username} : new Object[]{username,requestedId});
        return rows.stream().findFirst().orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy hồ sơ"));
    }
    public record Profile(long customerId,String customerCode,String fullName,String phone,String email) {}
}

package com.tivarek.week8;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    public DbUserDetailsService(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var rows=jdbc.query("""
            SELECT LoginName,PasswordHash,IsActive FROM dbo.Users WHERE LoginName=?
            """, (rs,n) -> new Account(rs.getString(1),rs.getString(2),rs.getBoolean(3)),username);
        if (rows.isEmpty()) throw new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ");
        Account a=rows.get(0);
        List<SimpleGrantedAuthority> authorities=jdbc.query("""
            SELECT DISTINCT 'ROLE_' + r.RoleCode AS Authority
            FROM dbo.Users u JOIN dbo.UserRoles ur ON ur.UserID=u.UserID
            JOIN dbo.Roles r ON r.RoleID=ur.RoleID WHERE u.LoginName=?
            UNION
            SELECT DISTINCT p.PermissionCode AS Authority
            FROM dbo.Users u JOIN dbo.UserRoles ur ON ur.UserID=u.UserID
            JOIN dbo.RolePermissions rp ON rp.RoleID=ur.RoleID
            JOIN dbo.Permissions p ON p.PermissionID=rp.PermissionID WHERE u.LoginName=?
            """,(rs,n) -> new SimpleGrantedAuthority(rs.getString(1)),a.login(),a.login());
        return User.withUsername(a.login()).password(a.hash()).disabled(!a.active())
                   .authorities(authorities).build();
    }
    private record Account(String login,String hash,boolean active) {}
}

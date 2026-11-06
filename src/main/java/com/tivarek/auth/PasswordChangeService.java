package com.tivarek.auth;

import com.tivarek.week8.AuditService;
import java.nio.charset.StandardCharsets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordChangeService {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public PasswordChangeService(JdbcTemplate db, PasswordEncoder encoder, AuditService audit) {
        this.db = db;
        this.encoder = encoder;
        this.audit = audit;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void change(String current, String next, String confirmation) {
        Authentication principal = SecurityContextHolder.getContext().getAuthentication();
        if (principal == null || !principal.isAuthenticated())
            throw new PasswordChangeProblem("Vui lòng đăng nhập lại.");
        if (current == null || current.isBlank() || current.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new PasswordChangeProblem("Mật khẩu hiện tại không đúng.");
        if (next == null || confirmation == null || !next.equals(confirmation))
            throw new PasswordChangeProblem("Hai mật khẩu mới không trùng nhau.");
        int bytes = next.getBytes(StandardCharsets.UTF_8).length;
        if (next.length() < 12 || next.length() > 72 || bytes > 72
                || next.codePoints().noneMatch(Character::isLetter)
                || next.codePoints().noneMatch(Character::isDigit))
            throw new PasswordChangeProblem("Mật khẩu mới cần từ 12 ký tự, gồm chữ và số, tối đa 72 byte UTF-8.");

        var existing = db.query("SELECT UserID,PasswordHash FROM dbo.Users WHERE LoginName=? AND IsActive=1",
            (rs, row) -> new PasswordRow(rs.getLong(1), rs.getString(2)), principal.getName());
        if (existing.size() != 1 || !encoder.matches(current, existing.getFirst().hash()))
            throw new PasswordChangeProblem("Mật khẩu hiện tại không đúng.");
        if (encoder.matches(next, existing.getFirst().hash()))
            throw new PasswordChangeProblem("Mật khẩu mới phải khác mật khẩu đang sử dụng.");
        int updated = db.update("UPDATE dbo.Users SET PasswordHash=? WHERE UserID=? AND PasswordHash=? AND IsActive=1",
            encoder.encode(next), existing.getFirst().userId(), existing.getFirst().hash());
        if (updated != 1) throw new PasswordChangeProblem("Tài khoản vừa thay đổi. Vui lòng thử lại.");
        audit.writeFor(principal, "PASSWORD_CHANGE", "USER", Long.toString(existing.getFirst().userId()),
            "Người dùng tự đổi mật khẩu");
    }

    private record PasswordRow(long userId, String hash) { }

    public static class PasswordChangeProblem extends RuntimeException {
        public PasswordChangeProblem(String message) { super(message); }
    }
}

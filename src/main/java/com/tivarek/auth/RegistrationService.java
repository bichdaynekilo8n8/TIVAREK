package com.tivarek.auth;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Đăng ký là một giao dịch: Users + UserRoles + Customers + AuditLogs. */
@Service
public class RegistrationService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public RegistrationService(JdbcTemplate jdbc,PasswordEncoder encoder) {
        this.jdbc=jdbc;this.encoder=encoder;
    }

    @Transactional(isolation=Isolation.SERIALIZABLE)
    public long register(RegistrationInput form) {
        if(form==null) throw new RegistrationException("Thông tin đăng ký không hợp lệ.");
        String username=form.username()==null?"":form.username().strip().toLowerCase(Locale.ROOT);
        String fullName=form.fullName()==null?"":form.fullName().strip();
        String phone=form.phone()==null?"":form.phone().strip();
        String email=form.email()==null||form.email().isBlank()?null:form.email().strip();
        String password=form.password();

        if(!username.matches("[a-z][a-z0-9._-]{3,29}") || fullName.isBlank() || fullName.length()>180 ||
            !phone.matches("\\+?[0-9]{9,15}") || (email!=null && (email.length()>254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))))
            throw new RegistrationException("Họ tên, tên đăng nhập, email hoặc số điện thoại không hợp lệ.");
        if(password==null || form.confirmPassword()==null || !password.equals(form.confirmPassword()))
            throw new RegistrationException("Hai mật khẩu không trùng nhau.");
        int bytes=password.getBytes(StandardCharsets.UTF_8).length;
        if(password.length()<12 || password.length()>72 || bytes>72 ||
            password.codePoints().noneMatch(Character::isLetter) ||
            password.codePoints().noneMatch(Character::isDigit))
            throw new RegistrationException("Mật khẩu cần ít nhất 12 ký tự, gồm chữ và số, tối đa 72 byte UTF-8.");

        // SERIALIZABLE bảo vệ hai yêu cầu đồng thời; luôn chỉ tạo role CUSTOMER phía server.
        Integer existing=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users WHERE LOWER(LoginName)=?",Integer.class,username);
        if(existing!=null && existing>0)
            throw new RegistrationException("Tên đăng nhập đã được sử dụng. Hãy chọn tên khác.");

        // Validate required reference data before any insert. SERIALIZABLE holds the role read
        // until commit; the insertion count below remains a second consistency check.
        Integer customerRoles=jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.Roles WHERE RoleCode='CUSTOMER'",Integer.class);
        if (customerRoles==null || customerRoles!=1) throw new RegistrationException(
            "Hệ thống đang thiếu cấu hình tài khoản khách hàng. Vui lòng liên hệ quản trị viên.");

        var key=new GeneratedKeyHolder();
        String hash=encoder.encode(password);
        jdbc.update(connection -> {
            PreparedStatement statement=connection.prepareStatement(
                "INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName,IsActive) VALUES (?,?,?,1)",new String[]{"UserID"});
            statement.setString(1,username);statement.setString(2,hash);statement.setString(3,fullName);
            return statement;
        },key);
        long userId=Objects.requireNonNull(key.getKey(),"Không nhận được UserID").longValue();

        int roles=jdbc.update("""
            INSERT INTO dbo.UserRoles(UserID,RoleID)
            SELECT ?,RoleID FROM dbo.Roles WHERE RoleCode='CUSTOMER'
            """,userId);
        // Fail gracefully if an administrator later removes the seed role.
        // The transaction rolls back the new Users row; never create an account without its role.
        if(roles!=1) throw new RegistrationException(
            "Hệ thống đang thiếu cấu hình tài khoản khách hàng. Vui lòng liên hệ quản trị viên.");

        String customerCode="REG-"+UUID.randomUUID().toString().replace("-","").substring(0,24).toUpperCase(Locale.ROOT);
        jdbc.update("INSERT INTO dbo.Customers(CustomerCode,FullName,Phone,Email,UserID) VALUES (?,?,?,?,?)",
            customerCode,fullName,phone,email,userId);
        jdbc.update("""
            INSERT INTO dbo.AuditLogs(ActorID,Action,EntityType,EntityKey,Summary)
            VALUES (?,'CUSTOMER_REGISTER','USER',?,N'Khách hàng tự đăng ký tài khoản')
            """,userId,Long.toString(userId));
        return userId;
    }

    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) {super(message);}
    }
}

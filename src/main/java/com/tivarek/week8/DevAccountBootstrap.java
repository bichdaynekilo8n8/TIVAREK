package com.tivarek.week8;

import java.nio.charset.StandardCharsets;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(name="tivarek.demo.enabled",havingValue="true")
public class DevAccountBootstrap implements CommandLineRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final String password;
    public DevAccountBootstrap(JdbcTemplate jdbc,PasswordEncoder encoder,
                               @Value("${TIVAREK_DEMO_PASSWORD:}") String password) {
        this.jdbc=jdbc; this.encoder=encoder; this.password=password;
    }
    @Override @Transactional
    public void run(String... args) {
        if (!"TIVAREK_WEEK8_DEV".equals(jdbc.queryForObject("SELECT DB_NAME()",String.class)))
            throw new IllegalStateException("Bootstrap chỉ dành cho Database thực hành TIVAREK_WEEK8_DEV");
        if (password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("TIVAREK_DEMO_PASSWORD cần tối thiểu 12 ký tự, tối đa 72 byte UTF-8");
        String[][] rows={{"w9.owner","OWNER"},{"w9.sales","SALES"},{"w9.tech","TECHNICIAN"},
                         {"w9.inventory","INVENTORY"},{"w9.customer","CUSTOMER"},{"w9.other","CUSTOMER"}};
        for (String[] row:rows) {
            Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Users WHERE LoginName=?",Integer.class,row[0]);
            // Không tự thay mật khẩu, thêm role hoặc liên kết lại tài khoản đã tồn tại.
            if (count!=null && count>0) continue;
            Long id=jdbc.queryForObject("""
                INSERT INTO dbo.Users(LoginName,PasswordHash,DisplayName,IsActive)
                OUTPUT INSERTED.UserID VALUES (?,?,?,1)
                """,Long.class,row[0],encoder.encode(password),"Thực hành "+row[1]);
            jdbc.update("INSERT INTO dbo.UserRoles(UserID,RoleID) SELECT ?,RoleID FROM dbo.Roles WHERE RoleCode=?",id,row[1]);
            if ("CUSTOMER".equals(row[1])) jdbc.update("""
                INSERT INTO dbo.Customers(CustomerCode,FullName,Phone,Email,UserID) VALUES (?,?,?,?,?)
                ""","W9-"+row[0],"Khách thực hành "+row[0],"0000000000",null,id);
            jdbc.update("""
                INSERT INTO dbo.AuditLogs(ActorID,Action,EntityType,EntityKey,Summary)
                VALUES (NULL,'DEV_ACCOUNT_CREATED','USER',?,N'Tạo tài khoản thực hành từ biến môi trường')
                """,row[0]);
        }
    }
}

package com.tivarek.week8;
import com.tivarek.TivarekApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SecurityDatabaseTest extends SecurityDatabaseContract {
    @Override void verifyDatabase() {
        assertTrue(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>)c -> c.getMetaData().getDatabaseProductName().equals("H2")));
    }
}

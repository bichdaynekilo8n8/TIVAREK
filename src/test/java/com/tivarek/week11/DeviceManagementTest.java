package com.tivarek.week11;
import com.tivarek.TivarekApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest(classes=TivarekApplication.class,properties="spring.datasource.url=jdbc:h2:mem:tivarek_week11;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class DeviceManagementTest extends DeviceManagementContract {
    @Override void verifyDatabase() { org.junit.jupiter.api.Assertions.assertNotNull(jdbc.queryForObject("SELECT 1",Integer.class)); }
}

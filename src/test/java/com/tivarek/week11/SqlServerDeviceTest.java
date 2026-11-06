package com.tivarek.week11;
import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
@SpringBootTest(classes=TivarekApplication.class,properties="tivarek.demo.enabled=false")
@AutoConfigureMockMvc @ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_SQLSERVER_TESTS",matches="true")
class SqlServerDeviceTest extends DeviceManagementContract {
    @Override void verifyDatabase() { org.junit.jupiter.api.Assertions.assertEquals("TIVAREK_WEEK8_DEV",jdbc.queryForObject("SELECT DB_NAME()",String.class)); }
}

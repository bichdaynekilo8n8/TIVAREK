package com.tivarek.week10;
import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes=TivarekApplication.class,properties="tivarek.demo.enabled=false")
@AutoConfigureMockMvc @ActiveProfiles("dev") @Transactional
@EnabledIfEnvironmentVariable(named="RUN_SQLSERVER_TESTS",matches="true")
class SqlServerProductTest extends ProductManagementContract {
    @BeforeEach void verifyDatabase() { assertEquals("TIVAREK_WEEK8_DEV",jdbc.queryForObject("SELECT DB_NAME()",String.class)); }
}

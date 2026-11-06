package com.tivarek.week10;
import com.tivarek.TivarekApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest(classes=TivarekApplication.class)
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ProductManagementTest extends ProductManagementContract {}

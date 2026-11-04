package com.tivarek.week8;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

// Không @Transactional trên test: phải quan sát COMMIT/ROLLBACK thật của service.
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_SQLSERVER_TESTS",matches="true")
class CustomerTransactionTest {
    @Autowired CustomerService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void onlyPracticeDatabase() {
        assertEquals("TIVAREK_WEEK8_DEV",jdbc.queryForObject("SELECT DB_NAME()",String.class));
    }

    @Test
    void addressFailureRollsBackAlreadyInsertedCustomer() {
        String code="TEST-"+UUID.randomUUID().toString().replace("-","");
        var invalidAddress=new AddressInput("x".repeat(251),"Xã mẫu","Quảng Ngãi");
        var request=new CustomerCreateRequest(code,"Khách thử rollback","0000000000",null,invalidAddress);
        // Gọi trực tiếp service qua bean Spring để bỏ qua HTTP validation,
        // cố ý làm INSERT địa chỉ bị DB từ chối sau INSERT khách.
        try {
            assertThrows(DataAccessException.class,() -> service.create(request));
            assertEquals(0L,jdbc.queryForObject(
                    "SELECT COUNT_BIG(*) FROM dbo.Customers WHERE CustomerCode=?",Long.class,code));
        } finally { cleanup(code); }
    }

    @Test
    void successfulTransactionCommitsBothCustomerAndAddress() {
        String code="TEST-"+UUID.randomUUID().toString().replace("-","");
        try {
            var request=new CustomerCreateRequest(code,"Khách thử commit","0000000000",null,
                    new AddressInput("Địa chỉ mẫu","Xã mẫu","Quảng Ngãi"));
            Customer result=service.create(request);
            assertNotNull(result.customerId());
            assertEquals(1L,jdbc.queryForObject("""
                    SELECT COUNT_BIG(*) FROM dbo.Customers c
                    JOIN dbo.CustomerAddresses a ON a.CustomerID=c.CustomerID
                    WHERE c.CustomerCode=? AND a.IsDefault=1
                    """,Long.class,code));
        } finally { cleanup(code); }
    }

    private void cleanup(String code) {
        jdbc.update("""
                DELETE a FROM dbo.CustomerAddresses a JOIN dbo.Customers c
                ON c.CustomerID=a.CustomerID WHERE c.CustomerCode=?
                """,code);
        jdbc.update("DELETE FROM dbo.Customers WHERE CustomerCode=?",code);
    }
}

package com.tivarek.week22;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class Week22ContentionAdviceTest {
    @Test void sqlContentionIsNeverMistakenForSuccessfulPayment() {
        var response=new TransactionContentionAdvice().retryLater(new CannotAcquireLockException("deadlock 1205"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,response.getStatusCode());
        assertEquals("2",response.getHeaders().getFirst("Retry-After"));
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("chưa được xác nhận"));
    }
}

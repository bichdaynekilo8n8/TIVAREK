package com.tivarek.week22;

import com.tivarek.week12.StockProblem;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Week22StockLocksTest {
    @Test void locksAreTransactionScopedAndSortedByIdentity() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        when(db.queryForList(startsWith("SELECT ProductID FROM dbo.Products WITH"),anyLong()))
            .thenAnswer(i->List.of(Map.of("ProductID",i.getArgument(1))));
        when(db.queryForList(startsWith("SELECT DeviceID FROM dbo.Devices WITH"),anyLong()))
            .thenAnswer(i->List.of(Map.of("DeviceID",i.getArgument(1))));
        boolean old=TransactionSynchronizationManager.isActualTransactionActive();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            StockLocks.all(db,List.of(8L,2L,8L),List.of(9L,3L,9L));
            var order=inOrder(db);
            order.verify(db).queryForList(startsWith("SELECT ProductID FROM dbo.Products WITH"),eq(2L));
            order.verify(db).queryForList(startsWith("SELECT ProductID FROM dbo.Products WITH"),eq(8L));
            order.verify(db).queryForList(startsWith("SELECT DeviceID FROM dbo.Devices WITH"),eq(3L));
            order.verify(db).queryForList(startsWith("SELECT DeviceID FROM dbo.Devices WITH"),eq(9L));
            verify(db,times(4)).queryForList(anyString(),anyLong());
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(old);
        }
    }

    @Test void lockCannotBeUsedWithoutTransaction() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        if(TransactionSynchronizationManager.isActualTransactionActive())
            fail("This unit test must not run inside an ambient transaction.");
        assertThrows(IllegalStateException.class,()->StockLocks.product(db,1));
        verifyNoInteractions(db);
    }

    @Test void missingProductIsRejectedBeforeStockMutation() {
        JdbcTemplate db=mock(JdbcTemplate.class);
        boolean old=TransactionSynchronizationManager.isActualTransactionActive();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            when(db.queryForList(startsWith("SELECT ProductID FROM dbo.Products WITH"),eq(1L))).thenReturn(List.of());
            assertThrows(StockProblem.class,()->StockLocks.product(db,1));
            verify(db,never()).update(anyString());
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(old);
        }
    }
}

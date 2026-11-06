package com.tivarek.week22;

import com.tivarek.TivarekApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Real database test: requires a dedicated SQL Server DEV database, never a production DB. */
@SpringBootTest(classes=TivarekApplication.class,properties="tivarek.demo.enabled=false")
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named="RUN_SQLSERVER_TESTS",matches="true")
class Week22SqlServerConcurrencyTest {
    @Autowired JdbcTemplate db;
    @Autowired PlatformTransactionManager txManager;

    private void verifyDev() {
        assertEquals("TIVAREK_WEEK8_DEV",db.queryForObject("SELECT DB_NAME()",String.class),
            "Không được chạy stress test trên database khác DEV.");
    }

    private TransactionTemplate transaction() {
        var tx=new TransactionTemplate(txManager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        tx.setTimeout(8);
        return tx;
    }

    @Test @Timeout(16)
    void productLockSerializesTwoIndependentConcurrentTransactions() throws Exception {
        verifyDev();
        List<Long> products=db.queryForList("SELECT TOP (1) ProductID FROM dbo.Products ORDER BY ProductID",Long.class);
        assumeTrue(!products.isEmpty(),"DEV database needs at least one test product");
        long productId=products.getFirst();
        var firstAcquired=new CountDownLatch(1);
        var releaseFirst=new CountDownLatch(1);
        var secondAcquired=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->transaction().execute(status->{
                StockLocks.product(db,productId);
                firstAcquired.countDown();
                try {
                    if(!releaseFirst.await(4,TimeUnit.SECONDS)) throw new IllegalStateException("First transaction timed out");
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();throw new IllegalStateException(e);
                }
                return 1;
            }));
            assertTrue(firstAcquired.await(5,TimeUnit.SECONDS));
            var second=pool.submit(()->transaction().execute(status->{
                StockLocks.product(db,productId);
                secondAcquired.countDown();
                return 2;
            }));
            assertFalse(secondAcquired.await(250,TimeUnit.MILLISECONDS),
                "Second transaction must wait for the first product lock");
            releaseFirst.countDown();
            assertEquals(Integer.valueOf(1),first.get(6,TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(2),second.get(6,TimeUnit.SECONDS));
            assertTrue(secondAcquired.getCount()==0);
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    @Test @Timeout(45)
    void twoFiveAndTenWorkersNeverEnterSameProductCriticalSectionTogether() throws Exception {
        verifyDev();
        List<Long> products=db.queryForList("SELECT TOP (1) ProductID FROM dbo.Products ORDER BY ProductID",Long.class);
        assumeTrue(!products.isEmpty(),"DEV database needs at least one test product");
        long productId=products.getFirst();
        for(int workers:new int[]{2,5,10}) {
            AtomicInteger inside=new AtomicInteger();
            AtomicInteger peak=new AtomicInteger();
            CountDownLatch start=new CountDownLatch(1);
            var pool=Executors.newFixedThreadPool(workers);
            try {
                var results=new java.util.ArrayList<java.util.concurrent.Future<?>>();
                for(int i=0;i<workers;i++) results.add(pool.submit(()->{
                    try {start.await();}
                    catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                    transaction().execute(status->{
                        StockLocks.product(db,productId);
                        int concurrent=inside.incrementAndGet();
                        peak.accumulateAndGet(concurrent,Math::max);
                        try {Thread.sleep(40);}
                        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                        finally {inside.decrementAndGet();}
                        return null;
                    });
                }));
                start.countDown();
                for(var result:results) result.get(20,TimeUnit.SECONDS);
                assertEquals(1,peak.get(),"Lost product exclusivity at "+workers+" threads");
            } finally {
                start.countDown();
                pool.shutdownNow();
            }
        }
    }

    @Test void transactionRollbackRemovesAllPartialWrites() {
        verifyDev();
        String code="W22_"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,24);
        assertThrows(IllegalStateException.class,()->transaction().execute(status->{
            db.update("INSERT INTO dbo.WarrantyPolicies(PolicyCode,Name,Terms) VALUES (?,?,?)",
                code,"Week 22 rollback probe","Not a real policy");
            throw new IllegalStateException("Force SQL rollback");
        }));
        assertEquals(0,db.queryForObject(
            "SELECT COUNT(*) FROM dbo.WarrantyPolicies WHERE PolicyCode=?",Integer.class,code));
    }

    @Test void schemaIncludesIdempotencyAndWeek22Constraints() {
        verifyDev();
        assertEquals(2,db.queryForObject("""
            SELECT COUNT(*) FROM sys.check_constraints
            WHERE name IN ('CK_W22_Payments_IdempotencyKey_NotBlank',
                           'CK_W22_SalesInventoryRequests_RequestKey_NotBlank')
            """,Integer.class));
        assertEquals(1,db.queryForObject("""
            SELECT COUNT(*) FROM sys.key_constraints
            WHERE name='UQ_Payments_2'
            """,Integer.class));
    }
}

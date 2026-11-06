package com.tivarek.week22;

import com.tivarek.week12.StockProblem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.TreeSet;

/**
 * One SQL Server update lock per stock identity, held until the caller's transaction commits.
 * Every reservation, issue, return and repair part consumption must acquire the same anchor
 * before calculating available quantity. Product locks are acquired before device locks,
 * each in ascending ID order, so multi-line sales use a stable lock order.
 */
public final class StockLocks {
    private StockLocks() { }

    public static void product(JdbcTemplate db, long productId) {
        requireTransaction();
        if (productId <= 0 || db.queryForList(
                "SELECT ProductID FROM dbo.Products WITH (UPDLOCK,HOLDLOCK) WHERE ProductID=?", productId).isEmpty())
            throw new StockProblem("Sản phẩm không tồn tại trong kho.");
    }

    public static void device(JdbcTemplate db, long deviceId) {
        requireTransaction();
        if (deviceId <= 0 || db.queryForList(
                "SELECT DeviceID FROM dbo.Devices WITH (UPDLOCK,HOLDLOCK) WHERE DeviceID=?", deviceId).isEmpty())
            throw new StockProblem("Thiết bị không tồn tại trong kho.");
    }

    public static void all(JdbcTemplate db, Collection<Long> products, Collection<Long> devices) {
        // TreeSet removes duplicate IDs and prevents reversed lock acquisition in bulk actions.
        for (long id : new TreeSet<>(products)) product(db, id);
        for (long id : new TreeSet<>(devices)) device(db, id);
    }

    /** Called after device() to release only the expired hold on this device. */
    public static void expireDeviceHold(JdbcTemplate db, long deviceId) {
        requireTransaction();
        db.update("""
            UPDATE dbo.DeviceReservations SET Status='EXPIRED'
            WHERE Status='ACTIVE' AND ExpiresAt<=SYSUTCDATETIME()
              AND ReservationID IN (SELECT ReservationID FROM dbo.ActiveDeviceReservations WHERE DeviceID=?)
            """,deviceId);
        db.update("""
            DELETE FROM dbo.ActiveDeviceReservations
            WHERE DeviceID=? AND ReservationID IN (
                SELECT ReservationID FROM dbo.DeviceReservations
                WHERE Status<>'ACTIVE' OR ExpiresAt<=SYSUTCDATETIME())
            """,deviceId);
    }

    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Thao tác khóa tồn kho phải nằm trong một giao dịch SQL.");
    }
}

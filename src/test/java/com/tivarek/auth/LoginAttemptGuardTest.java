package com.tivarek.auth;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptGuardTest {
    @Test void fiveFailuresFromSameUserAndIpRequireCoolingOff() {
        var time = new AtomicLong(1_000_000L);
        var guard = new LoginAttemptGuard(time::get);
        for (int i = 0; i < 4; i++) {
            assertFalse(guard.blocked("alice", "127.0.0.1"));
            guard.failed(" Alice ", "127.0.0.1");
        }
        guard.failed("ALICE", "127.0.0.1");
        assertTrue(guard.blocked("alice", "127.0.0.1"));
        assertFalse(guard.blocked("bob", "127.0.0.1"));
        time.addAndGet(LoginAttemptGuard.BLOCK_MS + 1);
        assertFalse(guard.blocked("alice", "127.0.0.1"));
        guard.failed("alice", "127.0.0.1");
        assertFalse(guard.blocked("alice", "127.0.0.1"));
        time.addAndGet(LoginAttemptGuard.WINDOW_MS + 1);
        assertFalse(guard.blocked("alice", "127.0.0.1"));
    }

    @Test void successClearsOnlyThatUsersFailures() {
        var guard = new LoginAttemptGuard();
        for (int i = 0; i < 5; i++) guard.failed("alice", "192.0.2.1");
        guard.failed("bob", "192.0.2.1");
        guard.succeeded("alice", "192.0.2.1");
        assertFalse(guard.blocked("alice", "192.0.2.1"));
        for (int i = 0; i < 4; i++) guard.failed("bob", "192.0.2.1");
        assertTrue(guard.blocked("bob", "192.0.2.1"));
    }

    @Test void repeatedFailureAcrossManyAccountsTripsIpLimit() {
        var guard = new LoginAttemptGuard();
        for (int i = 0; i < 30; i++) guard.failed("candidate" + i, "198.51.100.20");
        assertTrue(guard.blocked("brandNew", "198.51.100.20"));
        assertFalse(guard.blocked("brandNew", "198.51.100.21"));
    }
}

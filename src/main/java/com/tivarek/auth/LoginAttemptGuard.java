package com.tivarek.auth;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

/**
 * Local, bounded login throttle for a single TIVAREK process. The IP is taken
 * from the servlet container, never from an untrusted X-Forwarded-For header.
 * Multi-node hosting requires a shared, atomic throttle store.
 */
@Component
public class LoginAttemptGuard {
    static final long WINDOW_MS = Duration.ofMinutes(15).toMillis();
    static final long BLOCK_MS = Duration.ofMinutes(5).toMillis();
    private static final int MAX_ENTRIES = 10_000;
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final AtomicLong operations = new AtomicLong();
    private final LongSupplier clock;

    public LoginAttemptGuard() { this(System::currentTimeMillis); }

    // Package-scoped for deterministic unit tests.
    LoginAttemptGuard(LongSupplier clock) { this.clock = clock; }

    public boolean blocked(String username, String remoteAddress) {
        long now = clock.getAsLong();
        String user = userKey(username, remoteAddress);
        String ip = ipKey(remoteAddress);
        if (blockedKey(user, now) || blockedKey(ip, now)) return true;
        if (attempts.size() < MAX_ENTRIES || attempts.containsKey(user) || attempts.containsKey(ip)) return false;
        cleanup(now);
        return attempts.size() >= MAX_ENTRIES;
    }

    public void failed(String username, String remoteAddress) {
        long now = clock.getAsLong();
        addFailure(userKey(username, remoteAddress), 5, now);
        addFailure(ipKey(remoteAddress), 30, now);
        if (operations.incrementAndGet() % 128 == 0) cleanup(now);
    }

    public void succeeded(String username, String remoteAddress) {
        attempts.remove(userKey(username, remoteAddress));
        // Other accounts may still be under attack from the same address.
    }

    private boolean blockedKey(String key, long now) {
        Attempt state = attempts.get(key);
        return state != null && state.blockUntil > now;
    }

    private void addFailure(String key, int threshold, long now) {
        if (attempts.size() >= MAX_ENTRIES && !attempts.containsKey(key)) cleanup(now);
        if (attempts.size() >= MAX_ENTRIES && !attempts.containsKey(key)) return;
        attempts.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart >= WINDOW_MS)
                return new Attempt(now, 1, 0);
            if (old.blockUntil > now) return old;
            // One fresh attempt is allowed after the cooldown expires.
            if (old.blockUntil > 0) return new Attempt(now, 1, 0);
            int count = old.failures + 1;
            return new Attempt(old.windowStart, count,
                count >= threshold ? now + BLOCK_MS : 0);
        });
    }

    private void cleanup(long now) {
        attempts.entrySet().removeIf(e ->
            now - e.getValue().windowStart >= WINDOW_MS && e.getValue().blockUntil <= now);
    }

    private static String userKey(String username, String remoteAddress) {
        String name = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
        if (name.length() > 100) name = "INVALID_LONG_USERNAME";
        return "USER:" + address(remoteAddress) + ":" + name;
    }

    private static String ipKey(String remoteAddress) { return "IP:" + address(remoteAddress); }

    private static String address(String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private record Attempt(long windowStart, int failures, long blockUntil) { }
}

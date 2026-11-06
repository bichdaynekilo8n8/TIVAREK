package com.tivarek.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class LoginThrottleFilterTest {
    @Test void blockedFormLoginReturns429WithoutReachingAuthentication() throws Exception {
        var guard = new LoginAttemptGuard();
        for (int i = 0; i < 5; i++) guard.failed("someone", "127.0.0.1");
        var request = new MockHttpServletRequest("POST", "/login");
        request.setServletPath("/login");
        request.setRemoteAddr("127.0.0.1");
        request.addParameter("username", "someone");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        new LoginThrottleFilter(guard).doFilter(request, response, chain);
        assertEquals(429, response.getStatus());
        assertEquals("300", response.getHeader("Retry-After"));
        assertNull(chain.getRequest());
    }

    @Test void ordinaryGetDoesNotGetBlocked() throws Exception {
        var guard = new LoginAttemptGuard();
        for (int i = 0; i < 5; i++) guard.failed("someone", "127.0.0.1");
        var request = new MockHttpServletRequest("GET", "/login");
        request.setServletPath("/login");
        var chain = new MockFilterChain();
        new LoginThrottleFilter(guard).doFilter(request, new MockHttpServletResponse(), chain);
        assertNotNull(chain.getRequest());
    }
}

package com.tivarek.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/** Applied only inside Spring Security's filter chain, before form login. */
public final class LoginThrottleFilter extends OncePerRequestFilter {
    private final LoginAttemptGuard attempts;

    public LoginThrottleFilter(LoginAttemptGuard attempts) { this.attempts = attempts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && "/login".equals(request.getServletPath())
                && attempts.blocked(request.getParameter("username"), request.getRemoteAddr())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "300");
            response.setHeader("Cache-Control", "no-store");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Quá nhiều lần đăng nhập. Vui lòng thử lại sau ít phút.");
            return;
        }
        chain.doFilter(request, response);
    }
}

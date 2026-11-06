package com.tivarek.config;

import com.tivarek.auth.LoginAttemptGuard;
import com.tivarek.auth.LoginThrottleFilter;
import com.tivarek.week8.AuditService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@org.springframework.context.annotation.Import({com.tivarek.week24.PublicSeo.class, com.tivarek.week24.PrivacyHeaders.class})
@EnableMethodSecurity
public class SecurityConfig {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);
    @Bean PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            private final BCryptPasswordEncoder bcrypt=new BCryptPasswordEncoder(12);
            @Override public String encode(CharSequence raw) { return bcrypt.encode(raw); }
            @Override public boolean matches(CharSequence raw,String encoded) {
                if (raw==null || raw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) return false;
                return bcrypt.matches(raw,encoded);
            }
            @Override public boolean upgradeEncoding(String encoded) { return bcrypt.upgradeEncoding(encoded); }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserDetailsService users,
                                             AuditService audit, LoginAttemptGuard loginAttempts,
                                             com.tivarek.week24.PrivacyHeaders privacyHeaders) throws Exception {
        String[] customers = {"/api/admin/customers", "/api/admin/customers/**",
                              "/api/week8/customers", "/api/week8/customers/**"};
        http.userDetailsService(users)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/robots.txt", "/sitemap.xml", "/api/csrf", "/api/public/products", "/san-pham", "/san-pham/**", "/media/public/devices/*/images/*", "/sua-chua", "/tra-cuu-sua-chua").permitAll()
                .requestMatchers(HttpMethod.POST, "/register", "/sua-chua", "/tra-cuu-sua-chua").permitAll()
                .requestMatchers(HttpMethod.GET, "/gio-hang", "/thanh-toan", "/don-hang/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/gio-hang/add", "/gio-hang/update", "/gio-hang/clear", "/thanh-toan").permitAll()
                .requestMatchers(HttpMethod.GET, "/tra-cuu-bao-hanh").permitAll()
                .requestMatchers(HttpMethod.POST, "/tra-cuu-bao-hanh", "/bao-hanh/yeu-cau").permitAll()
                .requestMatchers("/doi-mat-khau").authenticated()
                .requestMatchers("/account/warranties", "/account/warranties/**").hasAuthority("WARRANTY_SELF_READ")
                .requestMatchers(HttpMethod.POST, "/admin/warranties/policies", "/admin/warranties/settings").hasAuthority("WARRANTY_POLICY_MANAGE")
                .requestMatchers(HttpMethod.POST, "/admin/warranties/*/token").hasAuthority("WARRANTY_TOKEN_ISSUE")
                .requestMatchers(HttpMethod.POST, "/admin/warranties/claims/*/status", "/admin/warranties/claims/*/repair").hasAuthority("WARRANTY_CLAIM_PROCESS")
                .requestMatchers("/admin/warranties", "/admin/warranties/**").hasAuthority("WARRANTY_READ")
                .requestMatchers(HttpMethod.GET,"/admin/reports").hasAuthority("REPORT_OVERVIEW")
                .requestMatchers(HttpMethod.GET,"/admin/reports/sales", "/admin/reports/export/sales").hasAuthority("REPORT_SALES")
                .requestMatchers(HttpMethod.GET,"/admin/reports/finance", "/admin/reports/export/finance").hasAuthority("REPORT_FINANCE")
                .requestMatchers(HttpMethod.GET,"/admin/reports/repairs", "/admin/reports/export/repairs").hasAuthority("REPORT_REPAIRS")
                .requestMatchers(HttpMethod.GET,"/admin/reports/stock", "/admin/reports/export/stock").hasAuthority("REPORT_STOCK")
                .requestMatchers(HttpMethod.GET,"/admin/reports/warranties", "/admin/reports/export/warranties").hasAuthority("REPORT_WARRANTIES")
                .requestMatchers("/admin/reports/**").denyAll()
                .requestMatchers("/admin/orders/new").hasAuthority("SALE_CREATE")
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/confirm", "/admin/orders/*/renew", "/admin/orders/*/cancel").hasAuthority("SALE_CONFIRM")
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/lines/add", "/admin/orders/*/lines/*/quantity").hasAuthority("SALE_CREATE")
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/ship").access((auth,ctx) ->
                    new org.springframework.security.authorization.AuthorizationDecision(auth.get().getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("SALE_FULFILL"))
                        && auth.get().getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("STOCK_ISSUE"))))
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/payment").hasAuthority("SALE_PAYMENT")
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/return").hasAuthority("SALE_RETURN")
                .requestMatchers(HttpMethod.POST, "/admin/orders/*/refund").hasAuthority("SALE_REFUND")
                .requestMatchers("/admin/orders", "/admin/orders/**").hasAuthority("SALE_READ")
                .requestMatchers("/account/orders", "/account/orders/**").hasAuthority("SALE_SELF_READ")
                .requestMatchers(HttpMethod.POST, "/admin/stock/*/reverse", "/admin/stock/locations", "/admin/stock/locations/*/status").hasAuthority("STOCK_ADJUST")
                .requestMatchers(HttpMethod.POST, "/admin/stock/suppliers").hasAuthority("STOCK_RECEIVE")
                .requestMatchers(HttpMethod.POST, "/admin/stock/reservations/**").hasAuthority("STOCK_ISSUE")
                .requestMatchers(HttpMethod.POST, "/admin/stock/draft", "/admin/stock/*/post").hasAnyAuthority("STOCK_RECEIVE","STOCK_ISSUE","STOCK_TRANSFER","STOCK_ADJUST")
                .requestMatchers("/admin/stock", "/admin/stock/**").hasAuthority("STOCK_READ")
                .requestMatchers(HttpMethod.POST, "/admin/repairs/intake", "/admin/repairs/requests/*/status").hasAuthority("REPAIR_INTAKE")
                .requestMatchers(HttpMethod.POST, "/admin/repairs/*/assign").hasAuthority("REPAIR_ASSIGN")
                .requestMatchers(HttpMethod.POST, "/admin/repairs/services", "/admin/repairs/*/quotes", "/admin/repairs/*/quotes/*/lines", "/admin/repairs/*/quotes/*/send", "/admin/repairs/*/quotes/*/respond").hasAuthority("REPAIR_QUOTE")
                .requestMatchers(HttpMethod.POST, "/admin/repairs/*/handover").hasAuthority("REPAIR_HANDOVER")
                .requestMatchers(HttpMethod.POST, "/admin/repairs/*/status", "/admin/repairs/*/work", "/admin/repairs/*/parts", "/admin/repairs/*/parts/device").hasAuthority("REPAIR_WORK")
                .requestMatchers(HttpMethod.POST, "/admin/repairs/*/images", "/admin/repairs/*/images/*/remove").hasAnyAuthority("REPAIR_INTAKE","REPAIR_WORK")
                .requestMatchers("/admin/repairs", "/admin/repairs/**").hasAuthority("REPAIR_READ")
                .requestMatchers("/account/repairs", "/account/repairs/**").hasAuthority("CUSTOMER_SELF_READ")
                .requestMatchers(HttpMethod.GET, "/admin/storefront").hasAnyAuthority("PRODUCT_WRITE","DEVICE_WRITE")
                .requestMatchers(HttpMethod.POST, "/admin/storefront/products/*/publication").hasAuthority("PRODUCT_WRITE")
                .requestMatchers(HttpMethod.GET, "/admin/storefront/products/*/images").hasAuthority("PRODUCT_WRITE")
                .requestMatchers(HttpMethod.POST, "/admin/storefront/products/*/images", "/admin/storefront/products/*/images/*/remove").hasAuthority("PRODUCT_WRITE")
                .requestMatchers(HttpMethod.POST, "/admin/storefront/devices/*/publication").hasAuthority("DEVICE_WRITE")
                .requestMatchers("/admin/devices/new", "/admin/devices/*/edit").hasAuthority("DEVICE_WRITE")
                .requestMatchers(HttpMethod.POST, "/admin/devices/*/checks", "/api/admin/devices/*/checks").hasAuthority("DEVICE_CHECK_WRITE")
                .requestMatchers(HttpMethod.POST, "/admin/devices/*/images", "/admin/devices/*/images/**", "/api/admin/devices/*/images").hasAuthority("DEVICE_IMAGE_WRITE")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/devices/*/images/*").hasAuthority("DEVICE_IMAGE_WRITE")
                .requestMatchers(HttpMethod.PUT, "/api/admin/devices/*/images/order").hasAuthority("DEVICE_IMAGE_WRITE")
                .requestMatchers(HttpMethod.GET, "/admin/devices", "/admin/devices/**", "/api/admin/devices", "/api/admin/devices/**").hasAuthority("DEVICE_READ")
                .requestMatchers(HttpMethod.POST, "/admin/devices", "/admin/devices/*", "/api/admin/devices").hasAuthority("DEVICE_WRITE")
                .requestMatchers(HttpMethod.PUT, "/api/admin/devices/*").hasAuthority("DEVICE_WRITE")
                .requestMatchers("/admin/products/new", "/admin/products/*/edit").hasAuthority("PRODUCT_WRITE")
                .requestMatchers(HttpMethod.POST, "/admin/products", "/admin/products/**").hasAuthority("PRODUCT_WRITE")
                .requestMatchers(HttpMethod.GET, "/admin/products", "/admin/products/**").hasAuthority("PRODUCT_READ")
                .requestMatchers("/admin/catalog/**").hasAuthority("CATALOG_WRITE")
                .requestMatchers(HttpMethod.GET, "/api/admin/products", "/api/admin/products/**").hasAuthority("PRODUCT_READ")
                .requestMatchers(HttpMethod.POST, "/api/admin/products", "/api/admin/products/**").hasAuthority("PRODUCT_WRITE")
                .requestMatchers(HttpMethod.PUT, "/api/admin/products/**").hasAuthority("PRODUCT_WRITE")
                .requestMatchers(HttpMethod.GET, "/api/admin/catalog/**").hasAnyAuthority("PRODUCT_READ", "CATALOG_WRITE")
                .requestMatchers(HttpMethod.POST, "/api/admin/catalog/**").hasAuthority("CATALOG_WRITE")
                .requestMatchers(HttpMethod.PUT, "/api/admin/catalog/**").hasAuthority("CATALOG_WRITE")
                .requestMatchers("/admin", "/admin/**").hasAuthority("ADMIN_ACCESS")
                .requestMatchers(HttpMethod.GET, customers).hasAuthority("CUSTOMER_READ")
                .requestMatchers(HttpMethod.POST, customers).hasAuthority("CUSTOMER_WRITE")
                .requestMatchers(HttpMethod.PUT, customers).hasAuthority("CUSTOMER_WRITE")
                .requestMatchers(HttpMethod.GET, "/api/admin/costs").hasAuthority("COST_READ")
                .requestMatchers(HttpMethod.GET, "/api/admin/audit").hasAuthority("AUDIT_READ")
                .requestMatchers(HttpMethod.GET, "/account", "/api/account/customer", "/api/account/customers/**")
                    .hasAuthority("CUSTOMER_SELF_READ")
                .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                .anyRequest().denyAll())
            .addFilterBefore(new LoginThrottleFilter(loginAttempts), UsernamePasswordAuthenticationFilter.class)
            .headers(h -> h.addHeaderWriter(privacyHeaders).addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "no-referrer")))
            .formLogin(f -> f.loginPage("/login").failureHandler((request,response,exception) -> {
                loginAttempts.failed(request.getParameter("username"), request.getRemoteAddr());
                audit.failedLogin(request.getParameter("username"));
                response.sendRedirect(request.getContextPath()+"/login?error");
            }).permitAll().successHandler((request,response,auth) -> {
                loginAttempts.succeeded(auth.getName(), request.getRemoteAddr());
                audit.writeFor(auth,"LOGIN_SUCCESS","USER",auth.getName(),"Đăng nhập thành công");
                boolean admin=auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN_ACCESS"));
                response.sendRedirect(request.getContextPath()+(admin ? "/admin" : "/account"));
            }))
            .logout(l -> l.addLogoutHandler((request,response,auth) -> {
                if (auth != null) audit.writeFor(auth,"LOGOUT","USER",auth.getName(),"Đăng xuất");
            }).logoutSuccessUrl("/login?logout").deleteCookies("JSESSIONID"))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request,response,exception) -> {
                    if (request.getRequestURI().startsWith(request.getContextPath()+"/api/"))
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED).commence(request,response,exception);
                    else new LoginUrlAuthenticationEntryPoint("/login").commence(request,response,exception);
                })
                .accessDeniedHandler((request,response,exception) -> {
                    try {
                        audit.deniedRequest(SecurityContextHolder.getContext().getAuthentication(),
                            request.getMethod(), request.getRequestURI().substring(request.getContextPath().length()));
                    } catch (RuntimeException logFailure) {
                        // Never turn an authorization denial into an internal server error.
                        LOG.warn("Unable to record access-denied audit event", logFailure);
                    }
                    response.setStatus(403);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("Bạn không có quyền thực hiện thao tác này hoặc CSRF token không hợp lệ.");
                }));
        // Giữ CSRF, session fixation protection và các security header mặc định.
        return http.build();
    }
}

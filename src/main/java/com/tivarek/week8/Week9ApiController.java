package com.tivarek.week8;

import java.util.Map;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class Week9ApiController {
    private final CatalogService catalog;
    private final AccountService accounts;
    private final AuditQueryService audit;
    public Week9ApiController(CatalogService catalog,AccountService accounts,AuditQueryService audit) {
        this.catalog=catalog; this.accounts=accounts; this.audit=audit;
    }
    @GetMapping("/api/csrf")
    public Map<String,String> csrf(CsrfToken t) {
        return Map.of("token",t.getToken(),"headerName",t.getHeaderName(),"parameterName",t.getParameterName());
    }
    @GetMapping("/api/me")
    public Map<String,Object> me(Authentication a) {
        return Map.of("username",a.getName(),"authorities",a.getAuthorities().stream().map(x -> x.getAuthority()).toList());
    }
    @GetMapping("/api/public/products")
    public List<CatalogService.ProductView> products() { return catalog.products(); }
    @GetMapping("/api/admin/costs")
    public List<CatalogService.CostView> costs() { return catalog.costs(); }
    @GetMapping("/api/admin/audit")
    public List<AuditQueryService.Entry> audit() { return audit.recent(); }
    @GetMapping("/api/account/customer")
    public AccountService.Profile mine(Authentication a) { return accounts.mine(a.getName(),null); }
    @GetMapping("/api/account/customers/{id}")
    public AccountService.Profile ownId(Authentication a,@PathVariable("id") long id) { return accounts.mine(a.getName(),id); }
}

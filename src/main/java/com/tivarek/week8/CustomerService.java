package com.tivarek.week8;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {
    private final CustomerRepository repository;
    private final AuditService audit;
    public CustomerService(CustomerRepository repository,AuditService audit) { this.repository=repository; this.audit=audit; }

    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Transactional(readOnly=true)
    public List<Customer> search(String q) { return repository.search(q); }

    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Transactional(readOnly=true)
    public Customer get(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy khách hàng"));
    }

    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    @Transactional(rollbackFor=Exception.class)
    public Customer create(CustomerCreateRequest r) {
        String code=r.customerCode().trim().toUpperCase(Locale.ROOT);
        String name=r.fullName().trim();
        String phone=r.phone().trim();
        long id=repository.insert(code,name,phone,nullable(r.email()));
        if (r.address()!=null) repository.insertDefaultAddress(id,name,phone,r.address());
        audit.writeCurrent("CUSTOMER_CREATE","CUSTOMER",Long.toString(id),"Tạo hồ sơ khách hàng");
        return repository.findById(id).orElseThrow();
    }

    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    @Transactional
    public Customer update(long id, CustomerUpdateRequest r) {
        int changed=repository.update(id,r.fullName().trim(),r.phone().trim(),nullable(r.email()));
        if (changed==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy khách hàng");
        audit.writeCurrent("CUSTOMER_UPDATE","CUSTOMER",Long.toString(id),"Cập nhật hồ sơ khách hàng");
        return repository.findById(id).orElseThrow();
    }

    private static String nullable(String s) { return s==null || s.isBlank() ? null : s.trim(); }
}

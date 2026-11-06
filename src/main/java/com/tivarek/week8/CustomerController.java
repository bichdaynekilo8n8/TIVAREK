package com.tivarek.week8;
import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/week8/customers", "/api/admin/customers"})
public class CustomerController {
    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service=service; }

    @GetMapping
    public List<Customer> list(@RequestParam(name="q",defaultValue="") String q) {
        return service.search(q);
    }
    @GetMapping("/{id}")
    public Customer get(@PathVariable("id") long id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerCreateRequest r) {
        Customer customer=service.create(r);
        return ResponseEntity.created(URI.create("/api/week8/customers/"+customer.customerId())).body(customer);
    }

    @PutMapping("/{id}")
    public Customer update(@PathVariable("id") long id,@Valid @RequestBody CustomerUpdateRequest r) {
        return service.update(id,r);
    }
}

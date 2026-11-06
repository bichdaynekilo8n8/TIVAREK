package com.tivarek.week10;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin")
public class ProductApiController {
    private final ProductManagementService products;private final LookupService lookups;
    public ProductApiController(ProductManagementService products,LookupService lookups) { this.products=products;this.lookups=lookups; }
    @GetMapping("/products")
    public SearchResult search(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long categoryId,
        @RequestParam(required=false) Long brandId,@RequestParam(required=false) String stockMode,
        @RequestParam(required=false) Boolean active,@RequestParam(defaultValue="0") int page) {
        var rows=products.search(q,categoryId,brandId,stockMode,active,page);
        return new SearchResult(rows.stream().limit(25).toList(),page,rows.size()>25);
    }
    @GetMapping("/products/{id}") public ProductView get(@PathVariable long id) { return products.get(id); }
    @PostMapping("/products") public ResponseEntity<ProductView> create(@Valid @RequestBody ProductInput input) {
        var saved=products.create(input);return ResponseEntity.created(URI.create("/api/admin/products/"+saved.productId())).body(saved);
    }
    @PutMapping("/products/{id}") public ProductView update(@PathVariable long id,@Valid @RequestBody ProductInput input) { return products.update(id,input); }
    @PostMapping("/products/{id}/status") public ResponseEntity<Void> status(@PathVariable long id,@Valid @RequestBody StatusInput input) { products.status(id,input);return ResponseEntity.noContent().build(); }
    @GetMapping("/catalog/{kind}") public List<LookupView> all(@PathVariable String kind) { return lookups.all(CatalogKind.parse(kind)); }
    @PostMapping("/catalog/{kind}") public ResponseEntity<LookupView> createLookup(@PathVariable String kind,@Valid @RequestBody LookupInput input) {
        var saved=lookups.create(CatalogKind.parse(kind),input);return ResponseEntity.status(201).body(saved);
    }
    @PutMapping("/catalog/{kind}/{id}") public LookupView updateLookup(@PathVariable String kind,@PathVariable long id,@Valid @RequestBody LookupInput input) { return lookups.update(CatalogKind.parse(kind),id,input); }
    @PostMapping("/catalog/{kind}/{id}/status") public ResponseEntity<Void> statusLookup(@PathVariable String kind,@PathVariable long id,@Valid @RequestBody StatusInput input) { lookups.status(CatalogKind.parse(kind),id,input);return ResponseEntity.noContent().build(); }
    public record SearchResult(List<ProductView> items,int page,boolean hasNext) {}
}

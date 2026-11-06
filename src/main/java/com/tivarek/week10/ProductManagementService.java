package com.tivarek.week10;
import java.util.*;
import com.tivarek.week8.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ProductManagementService {
    private final ManagedProductRepository products;
    private final LookupRepository lookups;
    private final AuditService audit;
    public ProductManagementService(ManagedProductRepository products,LookupRepository lookups,AuditService audit) { this.products=products;this.lookups=lookups;this.audit=audit; }
    @PreAuthorize("hasAuthority('PRODUCT_READ')") @Transactional(readOnly=true)
    public List<ProductView> search(String q,Long categoryId,Long brandId,String mode,Boolean active,int page) {
        if(page<0||page>100000||q.length()>100) throw CatalogProblem.invalid("Từ khóa tối đa 100 ký tự; số trang không hợp lệ.");
        if(mode!=null&&!mode.isBlank()&&!Set.of("BULK","SERIALIZED").contains(mode)) throw CatalogProblem.invalid("Loại quản lý không hợp lệ.");
        return products.search(q.strip(),categoryId,brandId,mode,active,page);
    }
    @PreAuthorize("hasAuthority('PRODUCT_READ')") @Transactional(readOnly=true)
    public ProductView get(long id) { return products.get(id); }
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')") @Transactional
    public ProductView create(ProductInput p) {
        references(p);long id=products.insert(p);
        audit.writeCurrent("PRODUCT_CREATE","PRODUCT",Long.toString(id),"Thêm "+p.sku()+" — "+p.name());return products.get(id);
    }
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')") @Transactional
    public ProductView update(long id,ProductInput p) {
        var old=products.get(id);
        if(!old.stockMode().equals(p.stockMode())) throw CatalogProblem.invalid("Loại quản lý được cố định khi tạo để giữ đúng dữ liệu thiết bị và kho. Hãy tạo SKU mới nếu cần đổi loại.");
        references(p);
        if(products.update(id,p)!=1) throw stale();
        audit.writeCurrent("PRODUCT_UPDATE","PRODUCT",Long.toString(id),"Sửa "+old.sku()+" → "+p.sku()+"; tên: "+old.name()+" → "+p.name()+"; giá: "+old.listPrice()+" → "+p.listPrice());return products.get(id);
    }
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')") @Transactional
    public void status(long id,StatusInput s) {
        var old=products.get(id);
        if(s.active()) references(old.input());
        if(products.status(id,s)!=1) throw stale();
        audit.writeCurrent(s.active()?"PRODUCT_REACTIVATE":"PRODUCT_DISCONTINUE","PRODUCT",Long.toString(id),(s.active()?"Kinh doanh lại ":"Ngừng kinh doanh ")+old.sku());
    }
    private void references(ProductInput p) { lookups.requireActive(CatalogKind.categories,p.categoryId());lookups.requireActive(CatalogKind.brands,p.brandId());lookups.requireActive(CatalogKind.units,p.unitId()); }
    static CatalogProblem stale() { return CatalogProblem.conflict("Dữ liệu đã được người khác thay đổi. Tải lại trang trước khi lưu."); }
}

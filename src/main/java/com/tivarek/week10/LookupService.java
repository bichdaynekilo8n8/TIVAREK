package com.tivarek.week10;
import java.util.List;
import com.tivarek.week8.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
@Service
public class LookupService {
    private final LookupRepository repo;private final AuditService audit;
    public LookupService(LookupRepository repo,AuditService audit) { this.repo=repo;this.audit=audit; }
    @PreAuthorize("hasAuthority('PRODUCT_READ') or hasAuthority('CATALOG_WRITE')") @Transactional(readOnly=true)
    public List<LookupView> all(CatalogKind k) { return repo.all(k); }
    @PreAuthorize("hasAuthority('CATALOG_WRITE')") @Transactional(readOnly=true)
    public LookupView get(CatalogKind k,long id) { return repo.get(k,id); }
    @PreAuthorize("hasAuthority('CATALOG_WRITE')") @Transactional
    public LookupView create(CatalogKind k,LookupInput p) {
        long id=repo.insert(k,p);audit.writeCurrent("CATALOG_CREATE",k.name().toUpperCase(java.util.Locale.ROOT),Long.toString(id),"Thêm "+k.label+": "+p.code()+" — "+p.name());return repo.get(k,id);
    }
    @PreAuthorize("hasAuthority('CATALOG_WRITE')") @Transactional
    public LookupView update(CatalogKind k,long id,LookupInput p) {
        var old=repo.get(k,id);
        if(repo.update(k,id,p)!=1) throw ProductManagementService.stale();
        audit.writeCurrent("CATALOG_UPDATE",k.name().toUpperCase(java.util.Locale.ROOT),Long.toString(id),"Sửa "+k.label+": "+old.code()+" → "+p.code()+"; "+old.name()+" → "+p.name());return repo.get(k,id);
    }
    @PreAuthorize("hasAuthority('CATALOG_WRITE')") @Transactional
    public void status(CatalogKind k,long id,StatusInput s) {
        repo.lock(k,id);
        if(!s.active()&&repo.used(k,id)) throw CatalogProblem.conflict("Còn sản phẩm đang kinh doanh sử dụng "+k.label.toLowerCase()+" này. Hãy chuyển hoặc ngừng các sản phẩm đó trước.");
        if(repo.status(k,id,s)!=1) throw ProductManagementService.stale();
        audit.writeCurrent(s.active()?"CATALOG_REACTIVATE":"CATALOG_DISABLE",k.name().toUpperCase(java.util.Locale.ROOT),Long.toString(id),(s.active()?"Dùng lại ":"Ngừng sử dụng ")+k.label+" #"+id);
    }
}

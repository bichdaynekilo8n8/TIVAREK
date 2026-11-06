package com.tivarek.week10;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
public class ProductPageController {
    private final ProductManagementService products;private final LookupService lookups;
    public ProductPageController(ProductManagementService products,LookupService lookups) { this.products=products;this.lookups=lookups; }
    @ModelAttribute public void identity(Authentication a,Model m) {
        m.addAttribute("username",a.getName());m.addAttribute("canReadDevices",has(a,"DEVICE_READ"));
        m.addAttribute("canWrite",has(a,"PRODUCT_WRITE"));m.addAttribute("canCatalog",has(a,"CATALOG_WRITE"));
    }
    @GetMapping("/admin/products") @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public String list(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long categoryId,
        @RequestParam(required=false) Long brandId,@RequestParam(defaultValue="") String stockMode,
        @RequestParam(required=false) Boolean active,@RequestParam(defaultValue="0") int page,Model m) {
        var rows=products.search(q,categoryId,brandId,stockMode,active,page);
        m.addAttribute("products",rows.stream().limit(25).toList());m.addAttribute("hasNext",rows.size()>25);
        m.addAttribute("q",q);m.addAttribute("categoryId",categoryId);m.addAttribute("brandId",brandId);
        m.addAttribute("stockMode",stockMode);m.addAttribute("active",active);m.addAttribute("page",page);
        dictionaries(m);return "products-admin";
    }
    @GetMapping("/admin/products/new") @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String createForm(Model m) { m.addAttribute("input",ProductInput.empty());return productForm(null,m); }
    @GetMapping("/admin/products/{id}/edit") @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String editForm(@PathVariable long id,Model m) { m.addAttribute("input",products.get(id).input());return productForm(id,m); }
    @PostMapping("/admin/products") @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String create(@Valid @ModelAttribute("input") ProductInput input,BindingResult errors,Model m,RedirectAttributes flash) {
        if(errors.hasErrors()) return productForm(null,m);
        try { products.create(input);flash.addFlashAttribute("success","Đã thêm sản phẩm.");return "redirect:/admin/products"; }
        catch(CatalogProblem|DataIntegrityViolationException e) { m.addAttribute("failure",message(e));return productForm(null,m); }
    }
    @PostMapping("/admin/products/{id}") @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String update(@PathVariable long id,@Valid @ModelAttribute("input") ProductInput input,BindingResult errors,Model m,RedirectAttributes flash) {
        if(errors.hasErrors()) return productForm(id,m);
        try { products.update(id,input);flash.addFlashAttribute("success","Đã cập nhật sản phẩm.");return "redirect:/admin/products"; }
        catch(CatalogProblem|DataIntegrityViolationException e) { m.addAttribute("failure",message(e));return productForm(id,m); }
    }
    @PostMapping("/admin/products/{id}/status") @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String status(@PathVariable long id,@Valid @ModelAttribute StatusInput input,BindingResult errors,RedirectAttributes flash) {
        if(errors.hasErrors()) flash.addFlashAttribute("failure","Trạng thái hoặc phiên bản không hợp lệ. Tải lại trang.");
        else try { products.status(id,input);flash.addFlashAttribute("success",input.active()?"Đã kinh doanh lại sản phẩm.":"Đã ngừng kinh doanh sản phẩm."); }
        catch(CatalogProblem|DataIntegrityViolationException e) { flash.addFlashAttribute("failure",message(e)); }
        return "redirect:/admin/products";
    }
    @GetMapping("/admin/catalog/{kind}") @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    public String catalog(@PathVariable String kind,@RequestParam(required=false) Long editId,Model m) {
        var k=CatalogKind.parse(kind);m.addAttribute("lookupInput",editId==null?LookupInput.empty():lookups.get(k,editId).input());
        return lookupForm(k,editId,m);
    }
    @PostMapping("/admin/catalog/{kind}") @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    public String createLookup(@PathVariable String kind,@Valid @ModelAttribute("lookupInput") LookupInput input,BindingResult errors,Model m,RedirectAttributes flash) {
        var k=CatalogKind.parse(kind);
        if(errors.hasErrors()) return lookupForm(k,null,m);
        try { lookups.create(k,input);flash.addFlashAttribute("success","Đã thêm "+k.label.toLowerCase()+".");return "redirect:/admin/catalog/"+kind; }
        catch(CatalogProblem|DataIntegrityViolationException e) { m.addAttribute("failure",message(e));return lookupForm(k,null,m); }
    }
    @PostMapping("/admin/catalog/{kind}/{id}") @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    public String updateLookup(@PathVariable String kind,@PathVariable long id,@Valid @ModelAttribute("lookupInput") LookupInput input,BindingResult errors,Model m,RedirectAttributes flash) {
        var k=CatalogKind.parse(kind);
        if(errors.hasErrors()) return lookupForm(k,id,m);
        try { lookups.update(k,id,input);flash.addFlashAttribute("success","Đã lưu thay đổi.");return "redirect:/admin/catalog/"+kind; }
        catch(CatalogProblem|DataIntegrityViolationException e) { m.addAttribute("failure",message(e));return lookupForm(k,id,m); }
    }
    @PostMapping("/admin/catalog/{kind}/{id}/status") @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    public String statusLookup(@PathVariable String kind,@PathVariable long id,@Valid @ModelAttribute StatusInput input,BindingResult errors,RedirectAttributes flash) {
        var k=CatalogKind.parse(kind);
        if(errors.hasErrors()) flash.addFlashAttribute("failure","Trạng thái hoặc phiên bản không hợp lệ. Tải lại trang.");
        else try { lookups.status(k,id,input);flash.addFlashAttribute("success","Đã thay đổi trạng thái."); }
        catch(CatalogProblem|DataIntegrityViolationException e) { flash.addFlashAttribute("failure",message(e)); }
        return "redirect:/admin/catalog/"+kind;
    }
    private String productForm(Long id,Model m) { m.addAttribute("editId",id);dictionaries(m);return "product-form"; }
    private String lookupForm(CatalogKind k,Long id,Model m) { m.addAttribute("kind",k.name());m.addAttribute("label",k.label);m.addAttribute("editId",id);m.addAttribute("rows",lookups.all(k));return "catalog-admin"; }
    private void dictionaries(Model m) { m.addAttribute("categories",lookups.all(CatalogKind.categories));m.addAttribute("brands",lookups.all(CatalogKind.brands));m.addAttribute("units",lookups.all(CatalogKind.units)); }
    private static boolean has(Authentication a,String p) { return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals(p)); }
    private static String message(RuntimeException e) {
        if(e instanceof CatalogProblem) return e.getMessage();
        if(e instanceof DuplicateKeyException) return "SKU hoặc mã đã tồn tại (không phân biệt hoa thường). Vui lòng dùng mã khác.";
        return "Dữ liệu không hợp lệ hoặc liên kết không tồn tại. Kiểm tra lại thông tin.";
    }
}

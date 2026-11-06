package com.tivarek.week8;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {
    private final CustomerService customers;
    private final CatalogService catalog;
    private final AccountService accounts;
    private final AuditQueryService audit;
    public PageController(CustomerService customers,CatalogService catalog,AccountService accounts,AuditQueryService audit) {
        this.customers=customers; this.catalog=catalog; this.accounts=accounts; this.audit=audit;
    }
    @GetMapping("/admin")
    public String admin(Authentication a,Model m,@RequestParam(name="q",defaultValue="") String q,
                        @RequestParam(name="editId",required=false) Long editId) {
        m.addAttribute("username",a.getName());
        m.addAttribute("q",q);
        m.addAttribute("canReadDevices",has(a,"DEVICE_READ"));
        m.addAttribute("canStock",has(a,"STOCK_READ"));
        m.addAttribute("canRepair",has(a,"REPAIR_READ"));
        m.addAttribute("canSales",has(a,"SALE_READ"));
        m.addAttribute("canWarranty",has(a,"WARRANTY_READ"));
        boolean canReport=has(a,"REPORT_OVERVIEW")||has(a,"REPORT_SALES")||has(a,"REPORT_FINANCE")
            ||has(a,"REPORT_REPAIRS")||has(a,"REPORT_STOCK")||has(a,"REPORT_WARRANTIES");
        m.addAttribute("canReport",canReport);
        m.addAttribute("reportLanding",has(a,"REPORT_OVERVIEW")?"/admin/reports":
            has(a,"REPORT_SALES")?"/admin/reports/sales":
            has(a,"REPORT_FINANCE")?"/admin/reports/finance":
            has(a,"REPORT_REPAIRS")?"/admin/reports/repairs":
            has(a,"REPORT_STOCK")?"/admin/reports/stock":"/admin/reports/warranties");
        m.addAttribute("canReadProducts",has(a,"PRODUCT_READ"));
        m.addAttribute("canPublishStorefront",has(a,"PRODUCT_WRITE")||has(a,"DEVICE_WRITE"));
        m.addAttribute("canManageCatalog",has(a,"CATALOG_WRITE"));
        m.addAttribute("canWriteCustomers",has(a,"CUSTOMER_WRITE"));
        m.addAttribute("canReadCustomers",has(a,"CUSTOMER_READ"));
        m.addAttribute("canReadCosts",has(a,"COST_READ"));
        m.addAttribute("canReadAudit",has(a,"AUDIT_READ"));
        if (has(a,"CUSTOMER_READ")) {
            m.addAttribute("customers",customers.search(q));
            if (editId!=null && has(a,"CUSTOMER_WRITE")) m.addAttribute("editing",customers.get(editId));
        }
        if (has(a,"COST_READ")) m.addAttribute("costs",catalog.costs());
        if (has(a,"AUDIT_READ")) m.addAttribute("logs",audit.recent());
        return "admin";
    }
    @PostMapping("/admin/customers")
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public String create(@Valid @ModelAttribute CustomerCreateRequest request,BindingResult errors,RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("failure","Kiểm tra mã khách, họ tên, số điện thoại và email.");
        } else {
            try { customers.create(request); flash.addFlashAttribute("success","Đã thêm khách hàng."); }
            catch (DataIntegrityViolationException e) { flash.addFlashAttribute("failure","Mã khách đã tồn tại hoặc dữ liệu không hợp lệ."); }
        }
        return "redirect:/admin#customers";
    }
    @PostMapping("/admin/customers/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_WRITE')")
    public String update(@PathVariable("id") long id,@Valid @ModelAttribute CustomerUpdateRequest request,
                         BindingResult errors,RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("failure","Kiểm tra họ tên, số điện thoại và email.");
            return "redirect:/admin?editId="+id+"#customer-form";
        }
        try { customers.update(id,request); flash.addFlashAttribute("success","Đã cập nhật khách hàng."); }
        catch (DataIntegrityViolationException e) { flash.addFlashAttribute("failure","Dữ liệu khách hàng không hợp lệ."); }
        return "redirect:/admin#customers";
    }
    @GetMapping("/account") public String account(Authentication a,Model m) {
        m.addAttribute("username",a.getName());
        m.addAttribute("profile",accounts.mine(a.getName(),null)); return "account";
    }
    private boolean has(Authentication a,String p) {
        return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals(p));
    }
}

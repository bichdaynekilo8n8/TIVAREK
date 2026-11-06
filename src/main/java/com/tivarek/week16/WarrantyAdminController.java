package com.tivarek.week16;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasAuthority('WARRANTY_READ')")
public class WarrantyAdminController {
    private final WarrantyService warranties;
    public WarrantyAdminController(WarrantyService warranties) {this.warranties=warranties;}

    @GetMapping("/admin/warranties")
    public String index(Model m,Authentication auth) {
        m.addAttribute("warranties",warranties.adminWarranties());
        m.addAttribute("claims",warranties.adminClaims());
        m.addAttribute("policies",warranties.policies());
        m.addAttribute("products",warranties.products());
        m.addAttribute("devices",warranties.devices());
        m.addAttribute("services",warranties.services());
        m.addAttribute("canPolicy",auth!=null && auth.getAuthorities().stream().anyMatch(a->"WARRANTY_POLICY_MANAGE".equals(a.getAuthority())));
        m.addAttribute("canToken",auth!=null && auth.getAuthorities().stream().anyMatch(a->"WARRANTY_TOKEN_ISSUE".equals(a.getAuthority())));
        m.addAttribute("canProcess",auth!=null && auth.getAuthorities().stream().anyMatch(a->"WARRANTY_CLAIM_PROCESS".equals(a.getAuthority())));
        return "warranty/admin";
    }

    @GetMapping("/admin/warranties/claims/{id}")
    public String claim(@PathVariable long id,Model m) {
        m.addAttribute("claim",warranties.adminClaim(id));return "warranty/admin-claim";
    }

    @PostMapping("/admin/warranties/policies")
    public String policy(@RequestParam String code,@RequestParam String name,@RequestParam String terms,RedirectAttributes flash) {
        return run(flash,()->warranties.savePolicy(code,name,terms),"Đã lưu chính sách bảo hành.");
    }

    @PostMapping("/admin/warranties/settings")
    public String configure(@RequestParam String kind,@RequestParam long referenceId,@RequestParam long policyId,
                            @RequestParam(defaultValue="0") int days,RedirectAttributes flash) {
        return run(flash,()->warranties.configure(kind,referenceId,policyId,days),"Đã lưu cấu hình bảo hành.");
    }

    @PostMapping("/admin/warranties/{id}/token")
    public String token(@PathVariable long id,RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("secretToken",warranties.renewToken(id));
            flash.addFlashAttribute("secretCode",warranties.codeFor(id));
            flash.addFlashAttribute("success","Mã xác minh mới chỉ hiển thị lần này. Hãy bàn giao riêng cho khách; mã cũ hết hiệu lực.");
        }catch(WarrantyProblem ex) {flash.addFlashAttribute("failure",ex.getMessage());}
        return "redirect:/admin/warranties";
    }

    @PostMapping("/admin/warranties/claims/{id}/status")
    public String status(@PathVariable long id,@RequestParam String next,@RequestParam String note,RedirectAttributes flash) {
        try {warranties.transition(id,next,note);flash.addFlashAttribute("success","Đã cập nhật yêu cầu.");}
        catch(WarrantyProblem ex){flash.addFlashAttribute("failure",ex.getMessage());}
        return "redirect:/admin/warranties/claims/"+id;
    }

    @PostMapping("/admin/warranties/claims/{id}/repair")
    public String repair(@PathVariable long id,@RequestParam long ticketId,RedirectAttributes flash) {
        try {warranties.attachRepair(id,ticketId);flash.addFlashAttribute("success","Đã liên kết phiếu sửa bảo hành.");}
        catch(WarrantyProblem ex){flash.addFlashAttribute("failure",ex.getMessage());}
        return "redirect:/admin/warranties/claims/"+id;
    }

    private String run(RedirectAttributes flash,Runnable action,String message) {
        try {action.run();flash.addFlashAttribute("success",message);}
        catch(WarrantyProblem ex){flash.addFlashAttribute("failure",ex.getMessage());}
        return "redirect:/admin/warranties";
    }
}

package com.tivarek.week16;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WarrantyController {
    private final WarrantyService warranties;
    public WarrantyController(WarrantyService warranties) {this.warranties=warranties;}

    @GetMapping("/tra-cuu-bao-hanh")
    public String lookupPage() {return "warranty/lookup";}

    @PostMapping("/tra-cuu-bao-hanh")
    public String lookup(@RequestParam String code,@RequestParam String token,HttpSession session,Model model) {
        if (failures(session) >= 10) {
            model.addAttribute("failure","Đã nhập sai quá nhiều lần trong phiên này. Hãy mở phiên mới hoặc liên hệ cửa hàng.");
            return "warranty/lookup";
        }
        try {
            model.addAttribute("warranty",warranties.lookup(code.strip().toUpperCase(java.util.Locale.ROOT),token.strip()));
            model.addAttribute("code",code.strip().toUpperCase(java.util.Locale.ROOT));
            model.addAttribute("token",token.strip());
            session.setAttribute("warrantyFailures",0);
            return "warranty/result";
        } catch (WarrantyProblem ex) {
            session.setAttribute("warrantyFailures",failures(session)+1);
            model.addAttribute("failure","Không tìm thấy phiếu với thông tin xác minh này.");
            return "warranty/lookup";
        }
    }

    @PostMapping("/bao-hanh/yeu-cau")
    public String claim(@RequestParam String code,@RequestParam String token,
                        @RequestParam int quantity,@RequestParam String problem,HttpSession session,Model model) {
        if (failures(session) >= 10) {
            model.addAttribute("failure","Đã vượt giới hạn xác minh trong phiên này.");
            return "warranty/lookup";
        }
        try {
            String claimCode=warranties.claim(code.strip().toUpperCase(java.util.Locale.ROOT),token.strip(),quantity,problem);
            model.addAttribute("claimCode",claimCode);
            return "warranty/claimed";
        }catch(WarrantyProblem ex) {
            model.addAttribute("failure",ex.getMessage());
            return "warranty/lookup";
        }
    }

    @GetMapping("/account/warranties")
    @PreAuthorize("hasAuthority('WARRANTY_SELF_READ')")
    public String mine(Authentication authentication,Model model) {
        model.addAttribute("warranties",warranties.myWarranties(authentication));
        return "warranty/mine";
    }

    @PostMapping("/account/warranties/claim")
    @PreAuthorize("hasAuthority('WARRANTY_SELF_READ')")
    public String claimOwned(@RequestParam long warrantyId,@RequestParam int quantity,@RequestParam String problem,
                             Authentication authentication,Model model) {
        try {
            model.addAttribute("claimCode",warranties.claimOwned(warrantyId,quantity,problem,authentication));
            return "warranty/claimed";
        }catch(WarrantyProblem ex) {
            model.addAttribute("failure",ex.getMessage());
            model.addAttribute("warranties",warranties.myWarranties(authentication));
            return "warranty/mine";
        }
    }

    private int failures(HttpSession session) {
        Object number=session.getAttribute("warrantyFailures");
        return number instanceof Number n ? n.intValue() : 0;
    }
}

package com.tivarek.auth;

import com.tivarek.auth.RegistrationService.RegistrationException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {
    private final RegistrationService registration;
    public RegistrationController(RegistrationService registration) {this.registration=registration;}

    @GetMapping("/register")
    public String form(Authentication auth,Model model) {
        if(loggedIn(auth)) return landing(auth);
        if(!model.containsAttribute("registration")) model.addAttribute("registration",RegistrationInput.empty());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationInput input,
                           BindingResult errors,Authentication auth,Model model,
                           RedirectAttributes flash) {
        if(loggedIn(auth)) return landing(auth);
        if(errors.hasErrors()) return "register";
        try {
            registration.register(input);
            flash.addFlashAttribute("success","Đăng ký thành công! Bạn có thể đăng nhập ngay.");
            return "redirect:/login?registered";
        } catch(RegistrationException e) {
            model.addAttribute("failure",e.getMessage());
        } catch(DataIntegrityViolationException e) {
            // Không lộ chi tiết SQL; lỗi UNIQUE do yêu cầu đăng ký đồng thời cũng được xử lý.
            model.addAttribute("failure","Tên đăng nhập đã tồn tại hoặc thông tin không hợp lệ. Vui lòng kiểm tra lại.");
        }
        return "register";
    }

    private static boolean loggedIn(Authentication a) {
        return a!=null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
    }
    private static String landing(Authentication a) {
        return "redirect:"+(a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ADMIN_ACCESS"))?"/admin":"/account");
    }
}

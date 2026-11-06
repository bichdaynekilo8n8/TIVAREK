package com.tivarek.auth;

import com.tivarek.auth.PasswordChangeService.PasswordChangeProblem;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordChangeController {
    private final PasswordChangeService passwords;

    public PasswordChangeController(PasswordChangeService passwords) { this.passwords = passwords; }

    @GetMapping("/doi-mat-khau")
    public String page() { return "auth/change-password"; }

    @PostMapping("/doi-mat-khau")
    public String change(@RequestParam String currentPassword, @RequestParam String newPassword,
                         @RequestParam String confirmPassword, HttpServletRequest request, Model model) {
        try {
            passwords.change(currentPassword, newPassword, confirmPassword);
            // Do not leave a previously authenticated session usable after credential rotation.
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            SecurityContextHolder.clearContext();
            return "redirect:/login?passwordChanged";
        } catch (PasswordChangeProblem e) {
            model.addAttribute("failure", e.getMessage());
            return "auth/change-password";
        }
    }
}

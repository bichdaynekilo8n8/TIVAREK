package com.tivarek.controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AuthPageController {
    @GetMapping("/login") public String login(Authentication a) {
        if(a!=null&&a.isAuthenticated()&&!(a instanceof AnonymousAuthenticationToken)) {
            boolean admin=a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ADMIN_ACCESS"));
            return "redirect:"+(admin?"/admin":"/account");
        }
        return "login";
    }
}

package com.tivarek.week24;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class SeoModelAdvice {
    private final PublicSeo seo;
    public SeoModelAdvice(PublicSeo seo) { this.seo=seo; }
    @ModelAttribute
    public void metadata(HttpServletRequest request, Model model) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        model.addAttribute("seoCanonical", seo.canonical(path));
        model.addAttribute("seoIndexable", seo.enabled() && seo.publicPage(path)
            && "GET".equals(request.getMethod()) && request.getQueryString()==null);
    }
}

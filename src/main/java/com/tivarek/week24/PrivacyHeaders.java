package com.tivarek.week24;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.stereotype.Component;

/** Explicit no-store even when a controller supplies its own cache headers. */
@Component
public class PrivacyHeaders implements HeaderWriter {
    private final PublicSeo seo;
    public PrivacyHeaders(PublicSeo seo) { this.seo=seo; }
    @Override public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        boolean read="GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        boolean asset=read && (response.getStatus()==200 || response.getStatus()==304)
            && path.matches("/(css|js|images)/[^?]*") && !path.contains("..");
        if (!asset) {
            response.setHeader("Cache-Control", "private, no-store, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        boolean index=read && response.getStatus()==200 && seo.enabled()
            && seo.publicPage(path) && request.getQueryString()==null;
        response.setHeader("X-Robots-Tag", index ? "index, follow" : "noindex, nofollow");
    }
}

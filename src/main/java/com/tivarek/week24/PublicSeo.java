package com.tivarek.week24;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Canonicals never use untrusted Host / X-Forwarded-Host headers. */
@Component
public class PublicSeo {
    private final String base;
    public PublicSeo(@Value("${tivarek.seo.base-url:}") String value) {
        String configured=value==null?"":value.strip();
        if (configured.isEmpty()) { base=""; return; }
        URI uri=URI.create(configured);
        if (!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null
                || uri.getRawQuery()!=null || uri.getRawFragment()!=null
                || !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
            throw new IllegalArgumentException("TIVAREK_PUBLIC_BASE_URL must be the public HTTPS origin without a path, credentials, query or fragment");
        base=configured.replaceAll("/+$", "");
    }
    public boolean enabled() { return !base.isEmpty(); }
    public boolean publicPage(String path) {
        return path.equals("/") || path.equals("/san-pham") || path.equals("/sua-chua")
            || path.matches("/san-pham/(phu-kien|thiet-bi)/[1-9][0-9]*");
    }
    public String canonical(String path) { return enabled() && publicPage(path) ? base+path : null; }
    public String base() { return base; }
}

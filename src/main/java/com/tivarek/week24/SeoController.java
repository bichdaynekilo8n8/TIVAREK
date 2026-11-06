package com.tivarek.week24;

import com.tivarek.week13.StorefrontRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class SeoController {
    private final PublicSeo seo;
    private final StorefrontRepository repository;
    public SeoController(PublicSeo seo, StorefrontRepository repository) { this.seo=seo; this.repository=repository; }
    @GetMapping(value="/robots.txt", produces="text/plain;charset=UTF-8")
    public String robots() {
        if (!seo.enabled()) return "User-agent: *\nDisallow: /\n";
        return "User-agent: *\nAllow: /\nSitemap: "+seo.base()+"/sitemap.xml\n";
    }
    @GetMapping(value="/sitemap.xml", produces="application/xml;charset=UTF-8")
    public ResponseEntity<String> sitemap() {
        if (!seo.enabled()) return ResponseEntity.notFound().build();
        var paths=repository.sitemapPaths();
        StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
        for (String path:java.util.List.of("/", "/san-pham", "/sua-chua")) append(xml,path);
        for (String path:paths) append(xml,path);
        return ResponseEntity.ok(xml.append("</urlset>").toString());
    }
    private void append(StringBuilder xml,String path) {
        xml.append("<url><loc>").append(HtmlUtils.htmlEscape(seo.base()+path)).append("</loc></url>");
    }
}

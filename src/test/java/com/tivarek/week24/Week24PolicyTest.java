package com.tivarek.week24;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class Week24PolicyTest {
    @Test void developmentIsNotIndexableAndConfiguredOriginCannotContainSecrets() {
        assertFalse(new PublicSeo("").enabled());
        for (String value : new String[]{"http://localhost:8080", "https://user:pass@shop.example", "https://shop.example/?token=secret", "https://shop.example/a", "https://shop.example/#x"})
            assertThrows(IllegalArgumentException.class, ()->new PublicSeo(value));
        var seo=new PublicSeo("https://shop.example/");
        assertEquals("https://shop.example/san-pham",seo.canonical("/san-pham"));
        assertNull(seo.canonical("/don-hang/1"));
        assertFalse(seo.publicPage("/san-pham/thiet-bi/-1"));
    }
    @Test void privateTokensErrorsAndPostResponsesNeverGetSharedCachingOrIndexing() {
        var writer=new PrivacyHeaders(new PublicSeo("https://shop.example"));
        for(String path:new String[]{"/gio-hang","/thanh-toan","/don-hang/1","/tra-cuu-sua-chua","/tra-cuu-bao-hanh","/api/csrf","/account","/admin","/media/public/devices/1/images/2"}) {
            var req=new MockHttpServletRequest("GET",path); req.setQueryString("token=secret");
            var res=new MockHttpServletResponse(); res.setHeader("Cache-Control","public, max-age=86400");
            writer.writeHeaders(req,res);
            assertEquals("private, no-store, max-age=0",res.getHeader("Cache-Control"),path);
            assertEquals("noindex, nofollow",res.getHeader("X-Robots-Tag"),path);
        }
        for(int status:new int[]{302,403,404,500}) {
            var req=new MockHttpServletRequest("GET","/san-pham/thiet-bi/1");
            var res=new MockHttpServletResponse();res.setStatus(status);writer.writeHeaders(req,res);
            assertEquals("noindex, nofollow",res.getHeader("X-Robots-Tag"));
        }
        var req=new MockHttpServletRequest("POST","/sua-chua");var res=new MockHttpServletResponse();writer.writeHeaders(req,res);
        assertEquals("noindex, nofollow",res.getHeader("X-Robots-Tag"));
    }
    @Test void onlySuccessfulStaticAssetsRetainTheirResourceCachePolicy() {
        var writer=new PrivacyHeaders(new PublicSeo("https://shop.example"));
        var req=new MockHttpServletRequest("GET","/css/week24.css");
        var res=new MockHttpServletResponse();res.setHeader("Cache-Control","max-age=3600, must-revalidate");
        writer.writeHeaders(req,res);assertTrue(res.getHeader("Cache-Control").contains("max-age=3600"));
        res.setStatus(404);writer.writeHeaders(req,res);assertTrue(res.getHeader("Cache-Control").contains("no-store"));
    }
}

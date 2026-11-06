package com.tivarek.week24;
import com.tivarek.TivarekApplication;
import com.tivarek.week13.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes=TivarekApplication.class, properties={"tivarek.seo.base-url=https://shop.example", "spring.datasource.url=jdbc:h2:mem:week24web;MODE=MSSQLServer;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Week24WebTest {
    @Autowired MockMvc mvc;
    @MockitoBean StorefrontService storefront;
    @MockitoBean StorefrontRepository repository;
    private final StorefrontProductView item=new StorefrontProductView("BULK",1,"P1","Cáp HDMI","Phụ kiện",null,null,BigDecimal.TEN,"/images/tivarek-icon.png",null,1,1,null);
    @BeforeEach void catalog() {
        when(storefront.search(any())).thenReturn(new StorefrontService.Page(List.of(item),1,0,1));
        when(storefront.categories()).thenReturn(List.of());when(storefront.brands()).thenReturn(List.of());
        when(storefront.bulkDetail(1)).thenReturn(new StorefrontService.BulkDetail(item,List.of(),List.of()));
        when(storefront.device(1)).thenReturn(new StorefrontDeviceView(item,"Máy đã qua sử dụng","PASS",List.of(),List.of()));
    }
    @Test void publicPagesRenderWithMetadataAndNoSharedHtmlCache() throws Exception {
        for(String path:new String[]{"/","/san-pham","/san-pham/phu-kien/1","/san-pham/thiet-bi/1","/sua-chua"})
            mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",containsString("no-store")))
                .andExpect(header().string("X-Robots-Tag","index, follow"))
                .andExpect(content().string(containsString("https://shop.example"+path)))
                .andExpect(content().string(containsString("Bỏ qua điều hướng")));
        mvc.perform(get("/san-pham").queryParam("q","laptop")).andExpect(status().isOk())
            .andExpect(header().string("X-Robots-Tag","noindex, nofollow"));
    }
    @Test void sitemapContainsOnlyPublicPathsAndRobotsUsesConfiguredOrigin() throws Exception {
        when(repository.sitemapPaths()).thenReturn(List.of("/san-pham/phu-kien/1"));
        mvc.perform(get("/robots.txt").header("Host","attacker.example")).andExpect(status().isOk())
            .andExpect(content().string(containsString("Sitemap: https://shop.example/sitemap.xml")));
        mvc.perform(get("/sitemap.xml")).andExpect(status().isOk())
            .andExpect(content().string(containsString("https://shop.example/san-pham/phu-kien/1")))
            .andExpect(content().string(not(containsString("gio-hang"))));
    }
    @Test void staticCacheAndSensitiveCsrfHeadersWorkThroughSecurityChain() throws Exception {
        mvc.perform(get("/css/week24.css")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control",containsString("max-age=3600")));
        for(String path:new String[]{"/api/csrf","/gio-hang","/thanh-toan","/tra-cuu-sua-chua","/tra-cuu-bao-hanh","/login"})
            mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",containsString("no-store")))
                .andExpect(header().string("X-Robots-Tag","noindex, nofollow"));
        mvc.perform(post("/sua-chua")).andExpect(status().isForbidden());
    }
}

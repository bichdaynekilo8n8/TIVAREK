package com.tivarek.week23;

import com.tivarek.controller.HomeController;
import com.tivarek.service.ProductService;
import com.tivarek.week13.StorefrontProductView;
import com.tivarek.week13.StorefrontSearch;
import com.tivarek.week13.StorefrontService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Week23HomeControllerTest {
    private final ProductService products = mock(ProductService.class);
    private final StorefrontService storefront = mock(StorefrontService.class);
    private final HomeController home = new HomeController(products, storefront);

    @Test
    void homepageDisplaysAtMostSixRealPublishedProducts() {
        var published = IntStream.rangeClosed(1, 9)
            .mapToObj(i -> new StorefrontProductView("BULK", i, "SKU-" + i,
                "Sản phẩm " + i, "Phụ kiện", null, null,
                new BigDecimal("90000"), null, null, 5, 1L, null))
            .toList();
        when(storefront.search(any(StorefrontSearch.class)))
            .thenReturn(new StorefrontService.Page(published, published.size(), 0, 1));

        var model = new ExtendedModelMap();
        assertEquals("index", home.home(model));
        @SuppressWarnings("unchecked")
        var featured = (List<StorefrontProductView>) model.get("featured");
        assertNotNull(featured);
        assertEquals(6, featured.size());
        assertEquals("SKU-1", featured.getFirst().code());
        verify(storefront).search(any(StorefrontSearch.class));
    }

    @Test
    void homepageShowsHonestEmptyCatalogWhenNothingPublished() {
        when(storefront.search(any(StorefrontSearch.class)))
            .thenReturn(new StorefrontService.Page(List.of(), 0, 0, 0));
        var model = new ExtendedModelMap();
        assertEquals("index", home.home(model));
        assertEquals(List.of(), model.get("featured"));
        assertTrue(model.containsKey("products"));
    }
}

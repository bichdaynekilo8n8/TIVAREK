package com.tivarek.controller;

import com.tivarek.service.ProductService;
import com.tivarek.week13.StorefrontSearch;
import com.tivarek.week13.StorefrontService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductService productService;
    private final StorefrontService storefrontService;

    public HomeController(
            ProductService productService,
            StorefrontService storefrontService
    ) {
        this.productService = productService;
        this.storefrontService = storefrontService;
    }

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute(
                "pageTitle",
                "TIVAREK — Điện tử & sửa chữa tại Quảng Ngãi"
        );

        model.addAttribute(
                "products",
                productService.getFeaturedProducts()
        );

        // Published, inventory-backed items. An empty catalog is a valid storefront state.
        var featured = storefrontService.search(new StorefrontSearch(
                "", null, null, "all", "", null, null, "newest", 0));
        model.addAttribute("featured", featured == null ? java.util.List.of() : featured.items().stream().limit(6).toList());

        return "index";
    }
}

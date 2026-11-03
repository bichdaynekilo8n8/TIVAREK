package com.tivarek.controller;

import com.tivarek.service.ProductService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final ProductService productService;

    public HomeController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String home(Model model) {

        model.addAttribute(
                "pageTitle",
                "TIVAREK — Thiết bị điện tử"
        );

        model.addAttribute(
                "products",
                productService.getFeaturedProducts()
        );

        return "index";
    }
}
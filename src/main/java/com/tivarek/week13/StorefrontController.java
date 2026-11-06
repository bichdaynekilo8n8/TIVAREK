package com.tivarek.week13;

import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/san-pham")
public class StorefrontController {
    private final StorefrontService service;
    public StorefrontController(StorefrontService service) {this.service=service;}

    @GetMapping
    public String list(@RequestParam(defaultValue="") String q,
                       @RequestParam(required=false) Long categoryId,
                       @RequestParam(required=false) Long brandId,
                       @RequestParam(defaultValue="all") String type,
                       @RequestParam(defaultValue="") String grade,
                       @RequestParam(required=false) BigDecimal minPrice,
                       @RequestParam(required=false) BigDecimal maxPrice,
                       @RequestParam(defaultValue="newest") String sort,
                       @RequestParam(defaultValue="0") int page,Model model) {
        var filter=new StorefrontSearch(q,categoryId,brandId,type,grade,minPrice,maxPrice,sort,page);
        model.addAttribute("filter",filter);
        model.addAttribute("data",service.search(filter));
        model.addAttribute("categories",service.categories());
        model.addAttribute("brands",service.brands());
        return "storefront/list";
    }

    @GetMapping("/phu-kien/{id}")
    public String bulk(@PathVariable long id,Model model) {
        var detail=service.bulkDetail(id);
        model.addAttribute("item",detail.item());
        model.addAttribute("specs",detail.specs());
        model.addAttribute("photos",detail.photos());
        return "storefront/bulk-detail";
    }

    @GetMapping("/thiet-bi/{id}")
    public String device(@PathVariable long id,Model model) {
        model.addAttribute("detail",service.device(id));
        return "storefront/device-detail";
    }
}

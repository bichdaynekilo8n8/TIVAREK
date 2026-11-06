package com.tivarek.week13;

import com.tivarek.week10.CatalogProblem;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Đăng / gỡ đăng không xóa hàng, lưu audit và dùng CSRF trên form. */
@Controller
@RequestMapping("/admin/storefront")
public class StorefrontAdminController {
    private final StorefrontService service;
    public StorefrontAdminController(StorefrontService service) {this.service=service;}

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRODUCT_WRITE','DEVICE_WRITE')")
    public String index(Model model,Authentication auth) {
        model.addAttribute("data",service.management());
        model.addAttribute("canPublishProducts",has(auth,"PRODUCT_WRITE"));
        model.addAttribute("canPublishDevices",has(auth,"DEVICE_WRITE"));
        model.addAttribute("username",auth.getName());
        return "storefront/manage";
    }

    @PostMapping("/products/{id}/publication")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String product(@PathVariable long id,@RequestParam boolean published,RedirectAttributes flash) {
        try {service.publishProduct(id,published);flash.addFlashAttribute("success","Đã cập nhật hiển thị sản phẩm.");}
        catch(CatalogProblem e) {flash.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/admin/storefront";
    }
    @GetMapping("/products/{id}/images")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String images(@PathVariable long id,Model model,Authentication auth) {
        model.addAttribute("productId",id);
        model.addAttribute("photos",service.manageProductImages(id));
        model.addAttribute("username",auth.getName());
        return "storefront/product-images";
    }
    @PostMapping("/products/{id}/images")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String addImage(@PathVariable long id,@RequestParam String url,RedirectAttributes flash) {
        try {service.addProductImage(id,url);flash.addFlashAttribute("success","Đã thêm ảnh.");}
        catch(CatalogProblem e) {flash.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/admin/storefront/products/"+id+"/images";
    }
    @PostMapping("/products/{id}/images/{imageId}/remove")
    @PreAuthorize("hasAuthority('PRODUCT_WRITE')")
    public String removeImage(@PathVariable long id,@PathVariable long imageId,RedirectAttributes flash) {
        try {service.removeProductImage(id,imageId);flash.addFlashAttribute("success","Đã gỡ ảnh.");}
        catch(CatalogProblem e) {flash.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/admin/storefront/products/"+id+"/images";
    }
    @PostMapping("/devices/{id}/publication")
    @PreAuthorize("hasAuthority('DEVICE_WRITE')")
    public String device(@PathVariable long id,@RequestParam boolean published,RedirectAttributes flash) {
        try {service.publishDevice(id,published);flash.addFlashAttribute("success","Đã cập nhật hiển thị thiết bị.");}
        catch(CatalogProblem e) {flash.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/admin/storefront";
    }
    private static boolean has(Authentication a,String p) {
        return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals(p));
    }
}

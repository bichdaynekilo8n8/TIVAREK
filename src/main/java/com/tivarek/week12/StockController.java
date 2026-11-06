package com.tivarek.week12;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/stock")
@PreAuthorize("hasAuthority('STOCK_READ')")
public class StockController {
    private final InventoryService service;
    public StockController(InventoryService service) { this.service=service; }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("locations",service.locations());
        model.addAttribute("suppliers",service.suppliers());
        model.addAttribute("items",service.items());
        model.addAttribute("devices",service.devices());
        model.addAttribute("documents",service.documents());
        model.addAttribute("balances",service.balances());
        model.addAttribute("devicePositions",service.devicePositions());
        model.addAttribute("reservations",service.reservations());
        model.addAttribute("serialReservations",service.serialReservations());
        return "stock-admin";
    }
    @GetMapping("/{id}")
    public String detail(@PathVariable long id,Model m) {
        m.addAttribute("doc",service.document(id));m.addAttribute("bulkLines",service.bulkLines(id));
        m.addAttribute("deviceLines",service.deviceLines(id));return "stock-detail";
    }

    @PostMapping("/draft")
    public String draft(@ModelAttribute StockCommand input,RedirectAttributes f) {
        try {long id=service.createDraft(input); f.addFlashAttribute("success","Đã tạo chứng từ nháp #"+id+". Kiểm tra rồi bấm Ghi sổ.");}
        catch(StockProblem|DataIntegrityViolationException e) {error(e,f);}
        return "redirect:/admin/stock";
    }
    @PostMapping("/{id}/post")
    public String post(@PathVariable long id,RedirectAttributes f) {
        try {service.post(id);f.addFlashAttribute("success","Đã ghi sổ chứng từ #"+id+".");}
        catch(StockProblem|DataIntegrityViolationException e) {error(e,f);}
        return "redirect:/admin/stock";
    }
    @PostMapping("/{id}/reverse")
    public String reverse(@PathVariable long id,@RequestParam String reason,RedirectAttributes f) {
        try {long reversal=service.reverse(id,reason);f.addFlashAttribute("success","Đã tạo phiếu đảo #"+reversal+". Sổ kho gốc được bảo toàn.");}
        catch(StockProblem|DataIntegrityViolationException e) {error(e,f);}
        return "redirect:/admin/stock";
    }
    @PostMapping("/locations")
    public String location(@RequestParam String code,@RequestParam String name,@RequestParam String type,RedirectAttributes f) {
        try{service.createLocation(code,name,type);f.addFlashAttribute("success","Đã thêm vị trí kho.");}
        catch(StockProblem|DataIntegrityViolationException e){error(e,f);}return "redirect:/admin/stock";
    }
    @PostMapping("/locations/{id}/status")
    public String locationStatus(@PathVariable long id,@RequestParam boolean active,RedirectAttributes f) {
        try{service.locationStatus(id,active);f.addFlashAttribute("success",active?"Đã mở lại vị trí kho.":"Đã đóng vị trí kho.");}
        catch(StockProblem|DataIntegrityViolationException e){error(e,f);}return "redirect:/admin/stock";
    }
    @PostMapping("/suppliers")
    public String supplier(@RequestParam String code,@RequestParam String name,@RequestParam String phone,RedirectAttributes f) {
        try{service.createSupplier(code,name,phone);f.addFlashAttribute("success","Đã thêm nhà cung cấp.");}
        catch(StockProblem|DataIntegrityViolationException e){error(e,f);}return "redirect:/admin/stock";
    }
    @PostMapping("/reservations/bulk")
    public String reserveBulk(@RequestParam long saleLineId,@RequestParam long locationId,@RequestParam int quantity,@RequestParam int minutes,RedirectAttributes f) {
        try {service.reserveBulk(saleLineId,locationId,quantity,minutes);f.addFlashAttribute("success","Đã giữ số lượng cho đơn bán.");}
        catch(StockProblem|DataIntegrityViolationException e){error(e,f);}return "redirect:/admin/stock";
    }
    @PostMapping("/reservations/device")
    public String reserveDevice(@RequestParam long saleLineId,@RequestParam int minutes,RedirectAttributes f) {
        try {service.reserveDevice(saleLineId,minutes);f.addFlashAttribute("success","Đã giữ đúng máy cho đơn bán.");}
        catch(StockProblem|DataIntegrityViolationException e){error(e,f);}return "redirect:/admin/stock";
    }
    @PostMapping("/reservations/{kind}/{id}/release")
    public String release(@PathVariable String kind,@PathVariable long id,RedirectAttributes f){
        try{service.release(kind.toUpperCase(java.util.Locale.ROOT),id);f.addFlashAttribute("success","Đã nhả giữ hàng.");}
        catch(StockProblem|DataIntegrityViolationException e){error(e,f);}return "redirect:/admin/stock";
    }
    private static void error(RuntimeException e,RedirectAttributes f) {f.addFlashAttribute("failure",e instanceof StockProblem?e.getMessage():"Dữ liệu trùng hoặc không hợp lệ. Kiểm tra lại liên kết.");}
}

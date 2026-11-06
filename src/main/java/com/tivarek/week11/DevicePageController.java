package com.tivarek.week11;
import java.math.BigDecimal;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.tivarek.week10.CatalogProblem;
@Controller
public class DevicePageController {
    private final DeviceService devices;private final DeviceImageService images;
    public DevicePageController(DeviceService devices,DeviceImageService images) { this.devices=devices;this.images=images; }
    @ModelAttribute public void identity(Authentication a,Model m) {
        m.addAttribute("username",a.getName());m.addAttribute("canWriteDevices",has(a,"DEVICE_WRITE"));m.addAttribute("canImages",has(a,"DEVICE_IMAGE_WRITE"));
        m.addAttribute("canCheck",has(a,"DEVICE_CHECK_WRITE"));m.addAttribute("canProducts",has(a,"PRODUCT_READ"));m.addAttribute("canCatalog",has(a,"CATALOG_WRITE"));
        m.addAttribute("grades",DeviceService.GRADES);m.addAttribute("results",Map.of("PASS","Đạt","FAIL","Không đạt","NEEDS_REPAIR","Cần sửa chữa"));
    }
    @GetMapping("/admin/devices") @PreAuthorize("hasAuthority('DEVICE_READ')")
    public String list(@RequestParam(defaultValue="") String q,@RequestParam(required=false) Long productId,@RequestParam(required=false) Long categoryId,
        @RequestParam(required=false) Long brandId,@RequestParam(defaultValue="") String grade,@RequestParam(required=false) BigDecimal minPrice,
        @RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="0") int page,Model m) {
        m.addAttribute("data",devices.search(q,productId,categoryId,brandId,grade,minPrice,maxPrice,page));
        m.addAttribute("q",q);m.addAttribute("productId",productId);m.addAttribute("categoryId",categoryId);m.addAttribute("brandId",brandId);
        m.addAttribute("grade",grade);m.addAttribute("minPrice",minPrice);m.addAttribute("maxPrice",maxPrice);
        m.addAttribute("products",devices.choices(productId));m.addAttribute("categories",devices.categories());m.addAttribute("brands",devices.brands());return "devices-admin";
    }
    @GetMapping("/admin/devices/new") @PreAuthorize("hasAuthority('DEVICE_WRITE')")
    public String createForm(@RequestParam(required=false) Long productId,Model m) {
        var d=DeviceInput.empty();m.addAttribute("input",new DeviceInput(d.assetTag(),productId,null,d.description(),d.conditionGrade(),null,d.faultDescription(),"",Map.of()));return form(null,m);
    }
    @GetMapping("/admin/devices/{id}") @PreAuthorize("hasAuthority('DEVICE_READ')")
    public String detail(@PathVariable long id,Model m) { m.addAttribute("detail",devices.get(id));return "device-detail"; }
    @GetMapping("/admin/devices/{id}/edit") @PreAuthorize("hasAuthority('DEVICE_WRITE')")
    public String editForm(@PathVariable long id,Model m) { m.addAttribute("input",devices.get(id).input());return form(id,m); }
    @PostMapping("/admin/devices") @PreAuthorize("hasAuthority('DEVICE_WRITE')")
    public String create(@Valid @ModelAttribute("input") DeviceInput input,BindingResult errors,Model m,RedirectAttributes flash) {
        if(errors.hasErrors()) return form(null,m);
        try { var result=devices.create(input);flash.addFlashAttribute("success","Đã tạo hồ sơ thiết bị. Chưa ghi nhận nhập kho.");return "redirect:/admin/devices/"+result.device().deviceId(); }
        catch(CatalogProblem|DataIntegrityViolationException e) { m.addAttribute("failure",message(e));return form(null,m); }
    }
    @PostMapping("/admin/devices/{id}") @PreAuthorize("hasAuthority('DEVICE_WRITE')")
    public String update(@PathVariable long id,@Valid @ModelAttribute("input") DeviceInput input,BindingResult errors,Model m,RedirectAttributes flash) {
        if(errors.hasErrors()) return form(id,m);
        try { devices.update(id,input);flash.addFlashAttribute("success","Đã lưu thiết bị.");return "redirect:/admin/devices/"+id; }
        catch(CatalogProblem|DataIntegrityViolationException e) { m.addAttribute("failure",message(e));return form(id,m); }
    }
    @PostMapping("/admin/devices/{id}/checks") @PreAuthorize("hasAuthority('DEVICE_CHECK_WRITE')")
    public String check(@PathVariable long id,@Valid @ModelAttribute CheckInput input,BindingResult errors,RedirectAttributes flash) {
        if(errors.hasErrors()) flash.addFlashAttribute("failure","Nhập nội dung kiểm tra, chọn kết quả hợp lệ và tải lại phiên bản mới nhất.");
        else try { devices.addCheck(id,input);flash.addFlashAttribute("success","Đã ghi kết quả kiểm tra."); }
        catch(CatalogProblem|DataIntegrityViolationException e) { flash.addFlashAttribute("failure",message(e)); }
        return "redirect:/admin/devices/"+id+"#checks";
    }
    @PostMapping("/admin/devices/{id}/images") @PreAuthorize("hasAuthority('DEVICE_IMAGE_WRITE')")
    public String upload(@PathVariable long id,@RequestParam List<MultipartFile> files,@RequestParam String version,RedirectAttributes flash) {
        try { images.upload(id,files,version);flash.addFlashAttribute("success","Đã tải ảnh lên."); }
        catch(CatalogProblem|DataIntegrityViolationException e) { flash.addFlashAttribute("failure",message(e)); }
        return "redirect:/admin/devices/"+id+"#photos";
    }
    @PostMapping("/admin/devices/{id}/images/{imageId}/remove") @PreAuthorize("hasAuthority('DEVICE_IMAGE_WRITE')")
    public String remove(@PathVariable long id,@PathVariable long imageId,@RequestParam String version,RedirectAttributes flash) {
        try { images.remove(id,imageId,version);flash.addFlashAttribute("success","Đã gỡ ảnh."); }
        catch(CatalogProblem|DataIntegrityViolationException e) { flash.addFlashAttribute("failure",message(e)); }return "redirect:/admin/devices/"+id+"#photos";
    }
    @PostMapping("/admin/devices/{id}/images/{imageId}/move") @PreAuthorize("hasAuthority('DEVICE_IMAGE_WRITE')")
    public String move(@PathVariable long id,@PathVariable long imageId,@RequestParam String action,@RequestParam String version,RedirectAttributes flash) {
        try { images.move(id,imageId,action,version);flash.addFlashAttribute("success","Đã thay đổi thứ tự ảnh."); }
        catch(CatalogProblem|DataIntegrityViolationException e) { flash.addFlashAttribute("failure",message(e)); }return "redirect:/admin/devices/"+id+"#photos";
    }
    private String form(Long id,Model m) {
        m.addAttribute("editId",id);m.addAttribute("productFixed",id!=null && devices.get(id).device().productId()!=0);DeviceInput input=(DeviceInput)m.getAttribute("input");
        m.addAttribute("products",devices.choices(input.productId()));m.addAttribute("definitions",devices.definitions());return "device-form";
    }
    private static boolean has(Authentication a,String authority) { return a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals(authority)); }
    static String message(RuntimeException e) { if(e instanceof CatalogProblem) return e.getMessage();if(e instanceof org.springframework.dao.DuplicateKeyException) return "Mã thiết bị đã tồn tại (không phân biệt hoa thường).";return "Dữ liệu không hợp lệ hoặc liên kết không tồn tại."; }
}

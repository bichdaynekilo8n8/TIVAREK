package com.tivarek.week14;

import com.tivarek.week12.InventoryService;
import com.tivarek.week12.StockProblem;
import com.tivarek.week10.CatalogProblem;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Controller
public class RepairController {
    /** Shared Vietnamese status labels for public tracking and staff-facing UI. */
    @ModelAttribute("repairStatusLabels")
    public java.util.Map<String,String> repairStatusLabels() {
        return java.util.Map.of(
            "RECEIVED", "Đã tiếp nhận", "DIAGNOSING", "Đang kiểm tra",
            "WAITING_APPROVAL", "Chờ duyệt báo giá", "REPAIRING", "Đang sửa chữa",
            "WAITING_PARTS", "Chờ linh kiện", "READY", "Sẵn sàng bàn giao",
            "DELIVERED", "Đã bàn giao", "CANCELLED", "Đã hủy");
    }
    private final RepairService repairs;
    private final InventoryService stock;
    private final RepairMediaService media;
    public RepairController(RepairService repairs,InventoryService stock,RepairMediaService media){this.repairs=repairs;this.stock=stock;this.media=media;}

    @GetMapping("/sua-chua")
    public String requestPage(){return "repair-request";}
    @PostMapping("/sua-chua")
    public String request(@RequestParam String name,@RequestParam String phone,@RequestParam String problem,@RequestParam(required=false) String preferredAt,RedirectAttributes flash){
        try{flash.addFlashAttribute("success","Đã tiếp nhận yêu cầu "+repairs.request(name,phone,problem,preferredAt)+". Cửa hàng sẽ liên hệ lại với bạn.");}
        catch(RepairProblem|DataIntegrityViolationException e){flash.addFlashAttribute("failure",message(e));}
        return "redirect:/sua-chua";
    }
    @GetMapping("/tra-cuu-sua-chua")
    public String tracking(){return "repair-tracking";}
    @PostMapping("/tra-cuu-sua-chua")
    public String track(@RequestParam String code,@RequestParam String token,Model model){
        try{model.addAttribute("ticket",repairs.track(code.strip().toUpperCase(java.util.Locale.ROOT),token.strip()));}
        catch(RepairProblem e){model.addAttribute("failure",e.getMessage());}
        return "repair-tracking";
    }
    @GetMapping("/account/repairs") @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    public String myTickets(Model m){m.addAttribute("tickets",repairs.ownTickets());m.addAttribute("quotes",repairs.ownQuotes());m.addAttribute("quoteLines",repairs.ownQuoteLines());return "repair-account";}
    @PostMapping("/account/repairs/quotes/{id}/respond") @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    public String myResponse(@PathVariable long id,@RequestParam boolean accept,RedirectAttributes f){
        try{repairs.customerResponse(id,accept);f.addFlashAttribute("success","Đã ghi nhận phản hồi báo giá.");}
        catch(RepairProblem e){f.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/account/repairs";
    }
    @GetMapping("/admin/repairs") @PreAuthorize("hasAuthority('REPAIR_READ')")
    public String admin(Model m){
        m.addAttribute("requests",repairs.requests());m.addAttribute("tickets",repairs.tickets());m.addAttribute("customers",repairs.customers());
        m.addAttribute("technicians",repairs.technicians());return "repairs-admin";
    }
    @GetMapping("/admin/repairs/{id}") @PreAuthorize("hasAuthority('REPAIR_READ')")
    public String detail(@PathVariable long id,Model m){
        m.addAttribute("ticket",repairs.ticket(id));m.addAttribute("history",repairs.history(id));m.addAttribute("quotes",repairs.quotes(id));
        m.addAttribute("lines",repairs.quoteLines(id));m.addAttribute("logs",repairs.workLogs(id));m.addAttribute("due",repairs.due(id));
        m.addAttribute("technicians",repairs.technicians());m.addAttribute("services",repairs.catalogServices());
        m.addAttribute("parts",repairs.parts());m.addAttribute("locations",stock.locations());m.addAttribute("stockDevices",stock.devices());m.addAttribute("images",media.list(id));return "repair-detail";
    }
    @PostMapping("/admin/repairs/requests/{id}/status")
    public String requestStatus(@PathVariable long id,@RequestParam String status,RedirectAttributes f){
        run(f,()->repairs.updateRequest(id,status),"Đã cập nhật yêu cầu.");return "redirect:/admin/repairs";
    }
    @PostMapping("/admin/repairs/intake")
    public String intake(@RequestParam(required=false) Long requestId,@RequestParam(required=false) Long customerId,
         @RequestParam(required=false) String newName,@RequestParam(required=false) String newPhone,
         @RequestParam String model,@RequestParam(required=false) String serial,
         @RequestParam String problem,@RequestParam String condition,@RequestParam(required=false) String accessories,
         @RequestParam(required=false) Long technicianId,RedirectAttributes f) {
        try{
            var receipt=repairs.intake(requestId,customerId,newName,newPhone,model,serial,problem,condition,accessories,technicianId);
            f.addFlashAttribute("receiptCode",receipt.ticketCode());f.addFlashAttribute("trackingToken",receipt.trackingCode());
            f.addFlashAttribute("success","Đã tạo phiếu. Hãy giao mã tra cứu bảo mật cho đúng khách hàng ngay bây giờ; mã không thể xem lại.");
            return "redirect:/admin/repairs/"+receipt.id();
        }catch(RepairProblem|DataIntegrityViolationException e){f.addFlashAttribute("failure",message(e));return "redirect:/admin/repairs";}
    }
    @PostMapping("/admin/repairs/{id}/assign")
    public String assign(@PathVariable long id,@RequestParam long technicianId,RedirectAttributes f){run(f,()->repairs.assign(id,technicianId),"Đã phân công.");return back(id);}
    @PostMapping("/admin/repairs/{id}/status")
    public String status(@PathVariable long id,@RequestParam String status,@RequestParam String note,@RequestParam(defaultValue="false") boolean publicNote,RedirectAttributes f){
        run(f,()->repairs.status(id,status,note,publicNote),"Đã cập nhật tiến độ.");return back(id);
    }
    @PostMapping("/admin/repairs/services")
    public String service(@RequestParam String code,@RequestParam String name,@RequestParam BigDecimal price,@RequestParam(defaultValue="30") int days,RedirectAttributes f){
        run(f,()->repairs.addService(code,name,price,days),"Đã thêm dịch vụ sửa chữa.");return "redirect:/admin/repairs";
    }
    @PostMapping("/admin/repairs/{id}/quotes")
    public String quote(@PathVariable long id,@RequestParam String kind,@RequestParam long referenceId,@RequestParam int quantity,@RequestParam BigDecimal price,RedirectAttributes f){
        run(f,()->repairs.quote(id,kind,referenceId,quantity,price),"Đã tạo báo giá nháp.");return back(id);
    }
    @PostMapping("/admin/repairs/{id}/quotes/{quoteId}/lines")
    public String addLine(@PathVariable long id,@PathVariable long quoteId,@RequestParam String kind,@RequestParam long referenceId,@RequestParam int quantity,@RequestParam BigDecimal price,RedirectAttributes f){
        run(f,()->repairs.addQuoteLine(id,quoteId,kind,referenceId,quantity,price),"Đã thêm dòng báo giá.");return back(id);
    }
    @PostMapping("/admin/repairs/{id}/quotes/{quoteId}/send")
    public String send(@PathVariable long id,@PathVariable long quoteId,RedirectAttributes f){run(f,()->repairs.sendQuote(id,quoteId),"Đã gửi báo giá.");return back(id);}
    @PostMapping("/admin/repairs/{id}/quotes/{quoteId}/respond")
    public String respond(@PathVariable long id,@PathVariable long quoteId,@RequestParam boolean accept,@RequestParam String evidence,RedirectAttributes f){
        run(f,()->repairs.quoteResponse(id,quoteId,accept,evidence),"Đã lưu phản hồi có bằng chứng.");return back(id);
    }
    @PostMapping("/admin/repairs/{id}/work")
    public String work(@PathVariable long id,@RequestParam String description,RedirectAttributes f){run(f,()->repairs.work(id,description),"Đã lưu công việc.");return back(id);}
    @PostMapping("/admin/repairs/{id}/parts")
    public String part(@PathVariable long id,@RequestParam long quoteItemId,@RequestParam long locationId,@RequestParam int quantity,RedirectAttributes f){
        run(f,()->repairs.consume(id,quoteItemId,locationId,quantity),"Đã ghi sổ sử dụng linh kiện.");return back(id);
    }
    @PostMapping("/admin/repairs/{id}/parts/device")
    public String devicePart(@PathVariable long id,@RequestParam long quoteItemId,@RequestParam long deviceId,@RequestParam long locationId,RedirectAttributes f){
        run(f,()->repairs.consumeDevice(id,quoteItemId,deviceId,locationId),"Đã ghi sổ linh kiện serial.");return back(id);
    }
    @PostMapping("/admin/repairs/{id}/images")
    public String uploadImage(@PathVariable long id,@RequestParam("file") MultipartFile image,RedirectAttributes f){
        try{media.upload(id,image);f.addFlashAttribute("success","Đã lưu ảnh hiện trạng máy.");}
        catch(RepairProblem|CatalogProblem|DataIntegrityViolationException e){f.addFlashAttribute("failure","Không lưu được ảnh. Chỉ nhận JPEG/PNG hợp lệ, tối đa 5 MB.");}
        return back(id);
    }
    @GetMapping("/admin/repairs/{id}/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable long id,@PathVariable long imageId){
        var c=media.load(id,imageId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .contentType(c.png()?MediaType.IMAGE_PNG:MediaType.IMAGE_JPEG).body(c.bytes());
    }
    @PostMapping("/admin/repairs/{id}/images/{imageId}/remove")
    public String removeImage(@PathVariable long id,@PathVariable long imageId,RedirectAttributes f){
        run(f,()->media.remove(id,imageId),"Đã gỡ ảnh.");return back(id);
    }
    @PostMapping("/admin/repairs/{id}/handover")
    public String handover(@PathVariable long id,@RequestParam BigDecimal amount,@RequestParam String method,@RequestParam String evidence,RedirectAttributes f){
        run(f,()->repairs.handover(id,amount,method,evidence),"Đã bàn giao và ghi nhận thanh toán.");return back(id);
    }
    private static String back(long id){return "redirect:/admin/repairs/"+id;}
    private static void run(RedirectAttributes f,Runnable op,String success){try{op.run();f.addFlashAttribute("success",success);}catch(RepairProblem|StockProblem|DataIntegrityViolationException e){f.addFlashAttribute("failure",message(e));}}
    private static String message(Exception e){return e instanceof RepairProblem||e instanceof StockProblem?e.getMessage():"Dữ liệu trùng hoặc không hợp lệ. Hãy kiểm tra lại.";}
}

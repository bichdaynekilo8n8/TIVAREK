package com.tivarek.week17;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mỗi báo cáo có quyền riêng, không cung cấp dữ liệu tài chính qua trang của nhân viên khác. */
@Controller
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) {this.service=service;}

    @GetMapping("/admin/reports")
    @PreAuthorize("hasAuthority('REPORT_OVERVIEW')")
    public String overview(@RequestParam(required=false) String from,@RequestParam(required=false) String to,
                           Authentication auth,Model model) {
        return page("overview",ReportRange.parse(from,to),auth,model);
    }

    @GetMapping("/admin/reports/sales")
    @PreAuthorize("hasAuthority('REPORT_SALES')")
    public String sales(@RequestParam(required=false) String from,@RequestParam(required=false) String to,
                        Authentication auth,Model model) {
        return page("sales",ReportRange.parse(from,to),auth,model);
    }

    @GetMapping("/admin/reports/finance")
    @PreAuthorize("hasAuthority('REPORT_FINANCE')")
    public String finance(@RequestParam(required=false) String from,@RequestParam(required=false) String to,
                          Authentication auth,Model model) {
        return page("finance",ReportRange.parse(from,to),auth,model);
    }

    @GetMapping("/admin/reports/repairs")
    @PreAuthorize("hasAuthority('REPORT_REPAIRS')")
    public String repairs(@RequestParam(required=false) String from,@RequestParam(required=false) String to,
                          Authentication auth,Model model) {
        return page("repairs",ReportRange.parse(from,to),auth,model);
    }

    @GetMapping("/admin/reports/stock")
    @PreAuthorize("hasAuthority('REPORT_STOCK')")
    public String stock(@RequestParam(required=false) String from,@RequestParam(required=false) String to,
                        Authentication auth,Model model) {
        return page("stock",ReportRange.parse(from,to),auth,model);
    }

    @GetMapping("/admin/reports/warranties")
    @PreAuthorize("hasAuthority('REPORT_WARRANTIES')")
    public String warranties(@RequestParam(required=false) String from,@RequestParam(required=false) String to,
                             Authentication auth,Model model) {
        return page("warranties",ReportRange.parse(from,to),auth,model);
    }

    private String page(String section,ReportRange r,Authentication auth,Model model) {
        model.addAttribute("section",section);
        model.addAttribute("range",r);
        model.addAttribute("canOverview",has(auth,"REPORT_OVERVIEW"));
        model.addAttribute("canSales",has(auth,"REPORT_SALES"));
        model.addAttribute("canFinance",has(auth,"REPORT_FINANCE"));
        model.addAttribute("canRepairs",has(auth,"REPORT_REPAIRS"));
        model.addAttribute("canStock",has(auth,"REPORT_STOCK"));
        model.addAttribute("canWarranties",has(auth,"REPORT_WARRANTIES"));
        switch(section) {
            case "overview" -> {
                model.addAttribute("overview",service.overview(r));
                model.addAttribute("daily",service.cashDays(r));
                model.addAttribute("topProducts",service.topProducts(r));
                model.addAttribute("repairQueue",service.repairQueue());
                model.addAttribute("lowStock",service.lowStock());
                model.addAttribute("expiring",service.expiringWarranties());
            }
            case "sales" -> {
                model.addAttribute("orders",service.orderStatuses(r));
                model.addAttribute("topProducts",service.topProducts(r));
                model.addAttribute("returns",service.returns(r));
                model.addAttribute("completedValue",service.completedOrderValue(r));
            }
            case "finance" -> {
                model.addAttribute("cash",service.cash(r));
                model.addAttribute("daily",service.cashDays(r));
            }
            case "repairs" -> {
                model.addAttribute("repairStatuses",service.repairStatuses(r));
                model.addAttribute("repairQueue",service.repairQueue());
            }
            case "stock" -> {
                model.addAttribute("lowStock",service.lowStock());
                model.addAttribute("lowStockCount",service.lowStockCount());
                model.addAttribute("stockLocations",service.stockByLocation());
                model.addAttribute("deviceCount",service.availableDevices());
            }
            case "warranties" -> {
                model.addAttribute("claimStatuses",service.claimStatuses(r));
                model.addAttribute("expiring",service.expiringWarranties());
                model.addAttribute("activeWarranties",service.activeWarranties());
            }
            default -> throw new IllegalArgumentException("Nhóm báo cáo không hợp lệ.");
        }
        return "week17/reports";
    }

    @GetMapping("/admin/reports/export/sales")
    @PreAuthorize("hasAuthority('REPORT_SALES')")
    public ResponseEntity<byte[]> exportSales(@RequestParam(required=false) String from,@RequestParam(required=false) String to) {
        return export("sales",ReportRange.parse(from,to));
    }
    @GetMapping("/admin/reports/export/finance")
    @PreAuthorize("hasAuthority('REPORT_FINANCE')")
    public ResponseEntity<byte[]> exportFinance(@RequestParam(required=false) String from,@RequestParam(required=false) String to) {
        return export("finance",ReportRange.parse(from,to));
    }
    @GetMapping("/admin/reports/export/repairs")
    @PreAuthorize("hasAuthority('REPORT_REPAIRS')")
    public ResponseEntity<byte[]> exportRepairs(@RequestParam(required=false) String from,@RequestParam(required=false) String to) {
        return export("repairs",ReportRange.parse(from,to));
    }
    @GetMapping("/admin/reports/export/stock")
    @PreAuthorize("hasAuthority('REPORT_STOCK')")
    public ResponseEntity<byte[]> exportStock(@RequestParam(required=false) String from,@RequestParam(required=false) String to) {
        return export("stock",ReportRange.parse(from,to));
    }
    @GetMapping("/admin/reports/export/warranties")
    @PreAuthorize("hasAuthority('REPORT_WARRANTIES')")
    public ResponseEntity<byte[]> exportWarranties(@RequestParam(required=false) String from,@RequestParam(required=false) String to) {
        return export("warranties",ReportRange.parse(from,to));
    }

    private ResponseEntity<byte[]> export(String section,ReportRange r) {
        ExportTable table=switch(section) {
            case "sales" -> new ExportTable(List.of("Mã hàng","Tên hàng","Số lượng","Giá trị bán trước hoàn trả"),
                List.of("SKU","ProductName","Quantity","GrossValue"),service.topProducts(r));
            case "finance" -> new ExportTable(List.of("Ngày (VN)","Thu bán","Thu sửa","Hoàn tiền","Chi phí","Dòng tiền ròng"),
                List.of("DayLocal","SalesIn","RepairsIn","Refunds","Expenses","NetCash"),service.cashDays(r));
            case "repairs" -> new ExportTable(List.of("Mã phiếu","Trạng thái","Nhận (VN)","Ngày chưa đóng","Trễ hẹn"),
                List.of("TicketCode","Status","ReceivedLocal","DaysOpen","Overdue"),service.repairQueue());
            case "stock" -> new ExportTable(List.of("SKU","Tên sản phẩm","Tồn thực tế","Đã giữ","Khả dụng"),
                List.of("SKU","ProductName","OnHand","Reserved","Available"),service.lowStock());
            case "warranties" -> new ExportTable(List.of("Mã bảo hành","Ngày hết hạn (VN)","Số ngày còn lại"),
                List.of("WarrantyCode","EndLocal","DaysRemaining"),service.expiringWarranties());
            default -> throw new IllegalArgumentException("Nhóm xuất báo cáo không hợp lệ.");
        };
        byte[] bytes=csv(table).getBytes(StandardCharsets.UTF_8);
        String filename="tivarek-"+section+"-"+r.from()+"-"+r.to()+".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment()
                .filename(filename,StandardCharsets.UTF_8).build().toString())
            .contentType(new MediaType("text","csv",StandardCharsets.UTF_8))
            .body(bytes);
    }

    private record ExportTable(List<String> headings,List<String> columns,List<Map<String,Object>> rows) {}

    /** UTF-8 BOM cho Excel; escape dấu nháy và vô hiệu công thức CSV injection. */
    static String csv(ExportTable table) {
        StringBuilder out=new StringBuilder("\uFEFF");
        out.append(String.join(",",table.headings())).append("\r\n");
        for(var row:table.rows()) {
            List<String> fields=new ArrayList<>();
            for(String key:table.columns()) fields.add(csvCell(row.get(key)));
            out.append(String.join(",",fields)).append("\r\n");
        }
        return out.toString();
    }

    static String csvCell(Object raw) {
        String text=raw==null?"":String.valueOf(raw).replace('\r',' ').replace('\n',' ').replace('\t',' ');
        String stripped=text.stripLeading();
        if(!(raw instanceof Number) && !stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0))>=0) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }

    private static boolean has(Authentication a,String authority) {
        return a!=null && a.getAuthorities().stream().anyMatch(g->authority.equals(g.getAuthority()));
    }
}

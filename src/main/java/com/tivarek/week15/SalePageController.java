package com.tivarek.week15;

import com.tivarek.week10.CatalogProblem;
import com.tivarek.week12.InventoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class SalePageController {
    private final SalesService sales;
    private final SaleWorkflowService workflow;
    private final InventoryService stock;
    public SalePageController(SalesService sales,SaleWorkflowService workflow,InventoryService stock) {
        this.sales=sales;this.workflow=workflow;this.stock=stock;
    }

    @GetMapping("/gio-hang")
    public String cart(HttpSession session,Model m) {
        try {m.addAttribute("cart",sales.preview(SaleCart.get(session)));}
        catch(CatalogProblem|SaleProblem e){m.addAttribute("failure",e.getMessage());m.addAttribute("cart",new SalesService.CartView(java.util.List.of(),BigDecimal.ZERO));}
        return "week15/cart";
    }
    @PostMapping("/gio-hang/add")
    public String add(@RequestParam String kind,@RequestParam long id,@RequestParam(defaultValue="1") int quantity,
                      HttpSession session,RedirectAttributes f) {
        try {
            var proposed=new LinkedHashMap<>(SaleCart.get(session));
            String key=SaleCart.key(kind,id);
            int previous=proposed.getOrDefault(key,0);
            if(quantity<1 || quantity>10000 || previous>10000-quantity)throw new SaleProblem("Số lượng không hợp lệ.");
            proposed.put(key,"DEVICE".equals(kind)?1:previous+quantity);
            var checked=sales.preview(proposed);
            for(var line:checked.lines())if(line.quantity()>line.available())throw new SaleProblem("Tồn kho hiện tại không đủ cho số lượng chọn.");
            SaleCart.put(session,kind,id,proposed.get(key));
            f.addFlashAttribute("success","Đã thêm sản phẩm vào giỏ hàng.");
        }catch(SaleProblem|CatalogProblem e){f.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/gio-hang";
    }
    @PostMapping("/gio-hang/update")
    public String update(@RequestParam String kind,@RequestParam long id,@RequestParam int quantity,
                         HttpSession session,RedirectAttributes f) {
        try {
            var proposed=new LinkedHashMap<>(SaleCart.get(session));
            if(quantity==0)proposed.remove(SaleCart.key(kind,id));
            else proposed.put(SaleCart.key(kind,id),quantity);
            var checked=sales.preview(proposed);
            for(var line:checked.lines())if(line.quantity()>line.available())throw new SaleProblem("Số lượng đang chọn vượt tồn khả dụng.");
            SaleCart.put(session,kind,id,quantity);
            f.addFlashAttribute("success","Đã cập nhật giỏ hàng.");
        }catch(SaleProblem|CatalogProblem e){f.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/gio-hang";
    }

    @PostMapping("/gio-hang/clear")
    public String clear(HttpSession session,RedirectAttributes f) {
        SaleCart.clear(session);f.addFlashAttribute("success","Đã xóa toàn bộ giỏ hàng.");
        return "redirect:/gio-hang";
    }

    @GetMapping("/thanh-toan")
    public String checkout(HttpSession session,Model m) {
        m.addAttribute("counter",false);
        return checkoutView(session,m);
    }
    @PostMapping("/thanh-toan")
    public String checkoutPost(@RequestParam String recipientName,@RequestParam String recipientPhone,
                               @RequestParam(required=false) String address,@RequestParam String fulfillmentMethod,
                               HttpSession session,Authentication auth,RedirectAttributes f) {
        return submit(false,null,recipientName,recipientPhone,address,fulfillmentMethod,session,auth,f);
    }
    @GetMapping("/don-hang/{code}")
    public String receipt(@PathVariable String code,@RequestParam(required=false) String token,Authentication auth,Model m) {
        m.addAttribute("detail",sales.guestDetail(code,token,auth));
        m.addAttribute("token",token);
        return "week15/receipt";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(@RequestParam(defaultValue="ALL") String status,Authentication auth,Model m) {
        m.addAttribute("orders",sales.adminOrders(status));m.addAttribute("status",status);
        m.addAttribute("canCreate",SalesService.has(auth,"SALE_CREATE"));
        return "week15/admin-orders";
    }
    @GetMapping("/admin/orders/new")
    public String counter(@RequestParam(defaultValue="") String customerQ,HttpSession session,Model m) {
        m.addAttribute("counter",true);
        m.addAttribute("customers",sales.customerOptions(customerQ));
        m.addAttribute("customerQ",customerQ);
        return checkoutView(session,m);
    }
    @PostMapping("/admin/orders/new")
    public String counterPost(@RequestParam String recipientName,@RequestParam String recipientPhone,
                              @RequestParam(required=false) String address,@RequestParam String fulfillmentMethod,
                              @RequestParam(required=false) Long counterCustomerId,
                              HttpSession session,Authentication auth,RedirectAttributes f) {
        return submit(true,counterCustomerId,recipientName,recipientPhone,address,fulfillmentMethod,session,auth,f);
    }
    @GetMapping("/admin/orders/{id}")
    public String adminOrder(@PathVariable long id,Authentication a,Model m) {
        m.addAttribute("detail",sales.detail(id));
        m.addAttribute("reservations",sales.activeReservations(id));
        m.addAttribute("locations",stock.locations());
        m.addAttribute("paymentKey",UUID.randomUUID().toString().replace("-",""));
        m.addAttribute("refundKey",UUID.randomUUID().toString().replace("-",""));
        m.addAttribute("shipKey",UUID.randomUUID().toString().replace("-",""));
        m.addAttribute("returnKey",UUID.randomUUID().toString().replace("-",""));
        m.addAttribute("canConfirm",SalesService.has(a,"SALE_CONFIRM"));
        m.addAttribute("canCreate",SalesService.has(a,"SALE_CREATE"));
        m.addAttribute("canShip",SalesService.has(a,"SALE_FULFILL") && SalesService.has(a,"STOCK_ISSUE"));
        m.addAttribute("canPay",SalesService.has(a,"SALE_PAYMENT"));
        m.addAttribute("canReturn",SalesService.has(a,"SALE_RETURN"));
        m.addAttribute("canRefund",SalesService.has(a,"SALE_REFUND"));
        return "week15/admin-detail";
    }
    @PostMapping("/admin/orders/{id}/confirm")
    public String confirm(@PathVariable long id,RedirectAttributes f) {return action(id,f,()->sales.confirm(id),"Đã xác nhận và giữ hàng.");}
    @PostMapping("/admin/orders/{id}/lines/add")
    public String addLine(@PathVariable long id,@RequestParam String kind,@RequestParam long itemId,
                          @RequestParam int quantity,RedirectAttributes f) {
        return action(id,f,()->sales.addDraftLine(id,kind,itemId,quantity),"Đã thêm dòng sản phẩm vào đơn nháp.");
    }
    @PostMapping("/admin/orders/{id}/lines/{lineId}/quantity")
    public String changeLine(@PathVariable long id,@PathVariable long lineId,@RequestParam int quantity,RedirectAttributes f) {
        return action(id,f,()->sales.changeDraftLine(id,lineId,quantity),"Đã cập nhật dòng đơn nháp.");
    }
    @PostMapping("/admin/orders/{id}/renew")
    public String renew(@PathVariable long id,RedirectAttributes f) {return action(id,f,()->sales.renewReservations(id),"Đã làm mới giữ hàng.");}
    @PostMapping("/admin/orders/{id}/cancel")
    public String cancel(@PathVariable long id,RedirectAttributes f) {return action(id,f,()->sales.cancel(id),"Đã hủy đơn và giải phóng hàng giữ.");}
    @PostMapping("/admin/orders/{id}/ship")
    public String ship(@PathVariable long id,@RequestParam long saleLineId,@RequestParam int quantity,
                       @RequestParam long locationId,@RequestParam(required=false) String carrier,
                       @RequestParam(required=false) String tracking,@RequestParam String idempotencyKey,RedirectAttributes f) {
        return action(id,f,()->workflow.ship(id,saleLineId,quantity,locationId,carrier,tracking,idempotencyKey),"Đã ghi sổ xuất và bàn giao hàng.");
    }
    @PostMapping("/admin/orders/{id}/payment")
    public String payment(@PathVariable long id,@RequestParam BigDecimal amount,@RequestParam String method,
                          @RequestParam(required=false) String providerReference,@RequestParam String idempotencyKey,
                          RedirectAttributes f) {
        return action(id,f,()->workflow.payment(id,amount,method,providerReference,idempotencyKey),"Đã ghi nhận thanh toán.");
    }
    @PostMapping("/admin/orders/{id}/return")
    public String receiveReturn(@PathVariable long id,@RequestParam long saleLineId,@RequestParam int quantity,
                                @RequestParam long destination,@RequestParam String reason,@RequestParam String idempotencyKey,RedirectAttributes f) {
        return action(id,f,()->workflow.receiveReturn(id,saleLineId,quantity,destination,reason,idempotencyKey),"Đã ghi sổ nhận hàng trả vào khu cách ly.");
    }
    @PostMapping("/admin/orders/{id}/refund")
    public String refund(@PathVariable long id,@RequestParam long originalPaymentId,@RequestParam BigDecimal amount,
                         @RequestParam String method,@RequestParam(required=false) String reference,
                         @RequestParam String idempotencyKey,RedirectAttributes f) {
        return action(id,f,()->workflow.refund(id,originalPaymentId,amount,method,reference,idempotencyKey),"Đã ghi nhận hoàn tiền.");
    }
    @GetMapping("/account/orders")
    public String myOrders(Authentication auth,Model m) {
        m.addAttribute("orders",sales.myOrders(auth));return "week15/my-orders";
    }
    @GetMapping("/account/orders/{code}")
    public String myOrder(@PathVariable String code,Authentication auth,Model m) {
        m.addAttribute("detail",sales.guestDetail(code,null,auth));m.addAttribute("token",null);
        return "week15/receipt";
    }

    private String checkoutView(HttpSession session,Model m) {
        try{var preview=sales.preview(SaleCart.get(session));m.addAttribute("cart",preview);
            if(preview.lines().isEmpty())m.addAttribute("failure","Giỏ hàng đang trống, vui lòng chọn sản phẩm.");
        }catch(SaleProblem|CatalogProblem e){m.addAttribute("failure",e.getMessage());m.addAttribute("cart",new SalesService.CartView(java.util.List.of(),BigDecimal.ZERO));}
        return "week15/checkout";
    }
    private String submit(boolean counter,Long counterCustomerId,String name,String phone,String address,String method,
                          HttpSession session,Authentication a,RedirectAttributes f) {
        try{
            SalesService.Receipt order=sales.create(SaleCart.get(session),name,phone,address,method,counter,counterCustomerId,a);
            SaleCart.clear(session);
            if(counter){f.addFlashAttribute("success","Đã tạo đơn tại quầy. Tiếp tục xác nhận và xuất hàng.");return "redirect:/admin/orders/"+order.id();}
            return "redirect:/don-hang/"+order.code()+"?token="+order.token();
        }catch(SaleProblem|CatalogProblem e){f.addFlashAttribute("failure",e.getMessage());return counter?"redirect:/admin/orders/new":"redirect:/thanh-toan";}
    }
    private String action(long id,RedirectAttributes f,Runnable task,String success) {
        try{task.run();f.addFlashAttribute("success",success);}
        catch(SaleProblem|com.tivarek.week12.StockProblem e){f.addFlashAttribute("failure",e.getMessage());}
        return "redirect:/admin/orders/"+id;
    }
    @ExceptionHandler(SaleProblem.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public String invalid(SaleProblem e,Model m) {
        m.addAttribute("message",e.getMessage());return "week15/sale-error";
    }
}

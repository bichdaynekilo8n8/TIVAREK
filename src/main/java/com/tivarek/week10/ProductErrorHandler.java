package com.tivarek.week10;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice(assignableTypes={ProductPageController.class,ProductApiController.class})
@Order(0)
public class ProductErrorHandler {
    @ExceptionHandler(CatalogProblem.class) @ResponseBody
    public ResponseEntity<Map<String,String>> problem(CatalogProblem e) { return ResponseEntity.status(e.getStatus()).body(Map.of("message",e.getMessage())); }
    @ExceptionHandler(DuplicateKeyException.class) @ResponseBody
    public ResponseEntity<Map<String,String>> duplicate(DuplicateKeyException e) { return ResponseEntity.status(409).body(Map.of("message","SKU hoặc mã đã tồn tại. Vui lòng dùng mã khác.")); }
    @ExceptionHandler(DataIntegrityViolationException.class) @ResponseBody
    public ResponseEntity<Map<String,String>> integrity(DataIntegrityViolationException e) { return ResponseEntity.badRequest().body(Map.of("message","Dữ liệu không hợp lệ hoặc liên kết không tồn tại.")); }
}

package com.tivarek.week11;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.tivarek.week10.CatalogProblem;
@ControllerAdvice(assignableTypes={DevicePageController.class,DeviceApiController.class}) @Order(-1)
public class DeviceErrorHandler {
    @ExceptionHandler(CatalogProblem.class) @ResponseBody
    public ResponseEntity<Map<String,String>> problem(CatalogProblem e) { return ResponseEntity.status(e.getStatus()).body(Map.of("message",e.getMessage())); }
    @ExceptionHandler(DataIntegrityViolationException.class) @ResponseBody
    public ResponseEntity<Map<String,String>> integrity(DataIntegrityViolationException e) { return ResponseEntity.status(e instanceof org.springframework.dao.DuplicateKeyException?409:400).body(Map.of("message",DevicePageController.message(e))); }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) @ResponseBody
    public ResponseEntity<Map<String,String>> status(org.springframework.web.server.ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Không thực hiện được yêu cầu.":e.getReason())); }
    @ExceptionHandler(MaxUploadSizeExceededException.class) @ResponseBody
    public ResponseEntity<Map<String,String>> oversized(MaxUploadSizeExceededException e) { return ResponseEntity.status(413).body(Map.of("message","Tệp tải lên quá lớn. Mỗi ảnh tối đa 5 MB; mỗi máy tối đa 10 ảnh.")); }
}

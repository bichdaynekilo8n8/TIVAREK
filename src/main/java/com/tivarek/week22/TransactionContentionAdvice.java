package com.tivarek.week22;

import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** A rolled-back SQL Server deadlock/lock timeout must not be reported as a successful sale. */
@ControllerAdvice
public class TransactionContentionAdvice {
    @ExceptionHandler(TransientDataAccessException.class)
    public ResponseEntity<String> retryLater(TransientDataAccessException ex) {
        // Do not automatically repeat POST requests: some can have external side effects.
        // Return a temporary failure; the same idempotency key must be kept if retried.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header(HttpHeaders.RETRY_AFTER, "2")
            .body("Hệ thống đang xử lý thao tác đồng thời. Giao dịch chưa được xác nhận thành công. "
                + "Vui lòng kiểm tra trạng thái đơn/phiếu trước khi thử lại cùng mã yêu cầu.");
    }
}

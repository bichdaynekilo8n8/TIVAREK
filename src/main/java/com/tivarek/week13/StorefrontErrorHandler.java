package com.tivarek.week13;

import com.tivarek.week10.CatalogProblem;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes={StorefrontController.class,PublicDeviceMediaController.class})
public class StorefrontErrorHandler {
    @ExceptionHandler(CatalogProblem.class)
    public ResponseEntity<String> catalogError(CatalogProblem error) {
        return ResponseEntity.status(error.getStatus()).contentType(MediaType.TEXT_PLAIN)
            .body(error.getMessage());
    }
}

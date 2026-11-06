package com.tivarek.week10;
import org.springframework.http.HttpStatus;
public class CatalogProblem extends RuntimeException {
    private final HttpStatus status;
    public CatalogProblem(HttpStatus status,String message) { super(message);this.status=status; }
    public HttpStatus getStatus() { return status; }
    public static CatalogProblem invalid(String message) { return new CatalogProblem(HttpStatus.BAD_REQUEST,message); }
    public static CatalogProblem conflict(String message) { return new CatalogProblem(HttpStatus.CONFLICT,message); }
    public static CatalogProblem missing() { return new CatalogProblem(HttpStatus.NOT_FOUND,"Không tìm thấy dữ liệu."); }
}

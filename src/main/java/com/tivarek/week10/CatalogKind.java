package com.tivarek.week10;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
// SQL identifiers chỉ lấy từ enum nội bộ, không nối tên bảng từ request.
public enum CatalogKind {
    categories("Categories","CategoryID","CategoryCode","Danh mục"),
    brands("Brands","BrandID","BrandCode","Thương hiệu"),
    units("Units","UnitID","UnitCode","Đơn vị tính");
    final String table,id,code;
    public final String label;
    CatalogKind(String table,String id,String code,String label) { this.table=table;this.id=id;this.code=code;this.label=label; }
    public String getLabel() { return label; }
    public static CatalogKind parse(String text) {
        try { return valueOf(text); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Loại danh mục không tồn tại"); }
    }
}

package com.tivarek.week13;

import com.tivarek.week10.CatalogProblem;
import java.math.BigDecimal;
import java.util.Set;

/** Các tham số công khai đều được kiểm tra trước khi ghép điều kiện SQL. */
public record StorefrontSearch(String q, Long categoryId, Long brandId, String type,
                               String grade, BigDecimal minPrice, BigDecimal maxPrice,
                               String sort, int page) {
    public static final int PAGE_SIZE = 12;
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999999999999");

    public StorefrontSearch {
        q = q == null ? "" : q.strip();
        type = type == null || type.isBlank() ? "all" : type;
        grade = grade == null ? "" : grade;
        sort = sort == null || sort.isBlank() ? "newest" : sort;
        if (q.length() > 100 || page < 0 || page > 10000 ||
            (categoryId != null && categoryId <= 0) || (brandId != null && brandId <= 0) ||
            !Set.of("all", "bulk", "device").contains(type) ||
            !Set.of("", "NEW", "LIKE_NEW", "GOOD", "FAIR", "FAULTY").contains(grade) ||
            !Set.of("newest", "price_asc", "price_desc", "name").contains(sort) ||
            (minPrice != null && minPrice.signum() < 0) ||
            (maxPrice != null && maxPrice.signum() < 0) ||
            (minPrice != null && minPrice.compareTo(MAX_PRICE) > 0) ||
            (maxPrice != null && maxPrice.compareTo(MAX_PRICE) > 0) ||
            (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0))
            throw CatalogProblem.invalid("Bộ lọc không hợp lệ. Kiểm tra giá, loại hàng, từ khóa và số trang.");
    }
}

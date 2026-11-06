package com.tivarek.week13;

import java.math.BigDecimal;
import java.net.URI;

/** Không chứa giá nhập, serial cá nhân hay thông tin tài khoản. */
public record StorefrontProductView(String kind, long id, String code, String name,
                                    String category, String brand, String grade,
                                    BigDecimal price, String sourceImage, Long coverImageId,
                                    int available, long categoryId, Long brandId) {
    public boolean inStock() { return available > 0; }
    public boolean device() { return "DEVICE".equals(kind); }
    public String detailUrl() { return device() ? "/san-pham/thiet-bi/" + id : "/san-pham/phu-kien/" + id; }
    public String imageUrl() {
        if (device()) return coverImageId == null ? null : "/media/public/devices/" + id + "/images/" + coverImageId;
        if (sourceImage == null || sourceImage.isBlank() || sourceImage.length() > 600 ||
            sourceImage.indexOf('\r') >= 0 || sourceImage.indexOf('\n') >= 0) return null;
        if (sourceImage.matches("(?i)/images/[a-z0-9/_-]+\\.(?:jpg|jpeg|png|webp|svg)")) return sourceImage;
        try {
            URI uri = URI.create(sourceImage);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null)
                return sourceImage;
        } catch (IllegalArgumentException ignored) { }
        return null;
    }
    public String gradeLabel() {
        if (grade == null) return "";
        return switch (grade) {
            case "NEW" -> "Mới";
            case "LIKE_NEW" -> "Như mới";
            case "GOOD" -> "Tốt";
            case "FAIR" -> "Đã sử dụng";
            case "FAULTY" -> "Cần sửa";
            default -> "";
        };
    }
}

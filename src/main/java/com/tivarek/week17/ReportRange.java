package com.tivarek.week17;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Bộ lọc ngày gồm cả ngày kết thúc, theo giờ Việt Nam; dữ liệu SQL lưu UTC. */
public record ReportRange(LocalDate from, LocalDate to) {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    public static ReportRange parse(String fromValue,String toValue) {
        LocalDate today = LocalDate.now(VIETNAM);
        try {
            LocalDate to = toValue == null || toValue.isBlank() ? today : LocalDate.parse(toValue);
            LocalDate from = fromValue == null || fromValue.isBlank() ? to.minusDays(29) : LocalDate.parse(fromValue);
            if (from.isAfter(to) || ChronoUnit.DAYS.between(from,to) > 365) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chọn khoảng ngày hợp lệ, tối đa 366 ngày.");
            }
            return new ReportRange(from,to);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ngày phải theo định dạng yyyy-MM-dd.");
        }
    }

    public Timestamp startUtc() {
        return Timestamp.valueOf(from.atStartOfDay(VIETNAM).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }

    public Timestamp endExclusiveUtc() {
        return Timestamp.valueOf(to.plusDays(1).atStartOfDay(VIETNAM).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }
}

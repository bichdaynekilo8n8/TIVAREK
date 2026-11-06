package com.tivarek.week17;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReportRangeTest {
    @Test void convertsVietnameseInclusiveDatesToUtcHalfOpenInterval() {
        var range=ReportRange.parse("2026-11-01","2026-11-02");
        assertEquals("2026-10-31 17:00:00.0",range.startUtc().toString());
        assertEquals("2026-11-02 17:00:00.0",range.endExclusiveUtc().toString());
        assertEquals(LocalDate.of(2026,11,2),range.to());
    }

    @Test void rejectsInvalidOrExpensiveDates() {
        assertThrows(ResponseStatusException.class,()->ReportRange.parse("2026-11-06","2026-11-05"));
        assertThrows(ResponseStatusException.class,()->ReportRange.parse("2025-01-01","2026-11-05"));
        assertThrows(ResponseStatusException.class,()->ReportRange.parse("2026-99-99","2026-11-05"));
    }
}

package com.tivarek.week17;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ReportCsvTest {
    @Test void escapesQuotesAndUntrustedSpreadsheetFormulas() {
        assertEquals("\"Laptop \"\"mới\"\"\"",ReportController.csvCell("Laptop \"mới\""));
        assertEquals("\"'=HYPERLINK(1)\"",ReportController.csvCell("=HYPERLINK(1)"));
        assertEquals("\"'  @CMD\"",ReportController.csvCell("  @CMD"));
        assertEquals("\"A B\"",ReportController.csvCell("A\nB"));
    }

    @Test void preservesGenuineNegativeNumericValues() {
        assertEquals("\"-500.50\"",ReportController.csvCell(new BigDecimal("-500.50")));
        assertEquals("\"'-500.50\"",ReportController.csvCell("-500.50"));
    }
}

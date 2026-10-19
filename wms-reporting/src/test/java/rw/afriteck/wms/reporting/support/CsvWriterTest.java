package rw.afriteck.wms.reporting.support;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CsvWriterTest {

    @Test
    void csvWriter_formatsRowsAndEscapesFormulaAndQuotes() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (CsvWriter csv = new CsvWriter(os)) {
            csv.row("Header1", "Header2", "Header3", "Header4");
            csv.row("Normal Text", 123, new BigDecimal("45.67"), true);
            csv.row("=SUM(A1:A2)", "+123", "-456", "@ADMIN");
            csv.row("Text with, comma", "Text with \"quotes\"", "Line1\nLine2", null);
        }

        byte[] bytes = os.toByteArray();
        // Check BOM
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("\uFEFFHeader1,Header2,Header3,Header4\r\n"));
        assertTrue(content.contains("Normal Text,123,45.67,true\r\n"));
        assertTrue(content.contains("'=SUM(A1:A2),'+123,'-456,'@ADMIN\r\n"));
        assertTrue(content.contains("\"Text with, comma\",\"Text with \"\"quotes\"\"\",\"Line1\nLine2\",\r\n"));
    }
}

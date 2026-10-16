package rw.afriteck.wms.reporting.support;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/** RFC 4180 CSV with a UTF-8 BOM (Excel). Text cells that start with = + - @ are prefixed with ' (formula injection). */
public final class CsvWriter implements Closeable {
    private final Writer out;

    public CsvWriter(OutputStream os) {
        this.out = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        try { out.write('\uFEFF'); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public CsvWriter row(Object... cells) {
        try {
            for (int i = 0; i < cells.length; i++) {
                if (i > 0) out.write(',');
                out.write(format(cells[i]));
            }
            out.write("\r\n");
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return this;
    }

    private static String format(Object v) {
        if (v == null) return "";
        if (v instanceof BigDecimal b) return b.toPlainString();
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        String s = v.toString();
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        return quote ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    @Override public void close() throws IOException { out.close(); }
}

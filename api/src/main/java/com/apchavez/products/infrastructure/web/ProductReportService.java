package com.apchavez.products.infrastructure.web;

import com.apchavez.products.application.ProductApplicationService;
import com.apchavez.products.domain.model.Product;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Generates the products PDF/Excel report used by GET /products/report/{pdf,excel}. */
@Service
public class ProductReportService {

    private static final String[] HEADERS = {"SKU", "Nombre", "Categoría", "Precio", "Stock", "Activo"};
    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 16f;
    private static final float[] COLUMN_X = {MARGIN, MARGIN + 90, MARGIN + 220, MARGIN + 340, MARGIN + 400, MARGIN + 450};

    private final ProductApplicationService applicationService;

    public ProductReportService(ProductApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public byte[] generatePdf() throws IOException {
        List<Product> products = applicationService.getAllProducts();
        double totalValue = totalInventoryValue(products);

        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDPage page = newPage(document);
            PDPageContentStream stream = newContentStream(document, page);
            float y = page.getMediaBox().getHeight() - MARGIN;

            y = writeTitle(stream, bold, y);
            y = writeHeaderRow(stream, bold, y);

            for (Product product : products) {
                if (y < MARGIN + ROW_HEIGHT) {
                    stream.close();
                    page = newPage(document);
                    stream = newContentStream(document, page);
                    y = page.getMediaBox().getHeight() - MARGIN;
                    y = writeHeaderRow(stream, bold, y);
                }
                writeRow(stream, font, y, product);
                y -= ROW_HEIGHT;
            }

            writeFooter(stream, bold, y, products.size(), totalValue);
            stream.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    public byte[] generateExcel() throws IOException {
        List<Product> products = applicationService.getAllProducts();
        double totalValue = totalInventoryValue(products);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            SXSSFSheet sheet = workbook.createSheet("Productos");

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i, CellType.STRING).setCellValue(HEADERS[i]);
            }

            int rowIndex = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0, CellType.STRING).setCellValue(product.sku());
                row.createCell(1, CellType.STRING).setCellValue(product.name());
                row.createCell(2, CellType.STRING).setCellValue(nullToEmpty(product.category()));
                row.createCell(3, CellType.NUMERIC).setCellValue(product.price());
                row.createCell(4, CellType.NUMERIC).setCellValue(product.stock());
                row.createCell(5, CellType.STRING).setCellValue(Boolean.TRUE.equals(product.active()) ? "Sí" : "No");
            }

            rowIndex++;
            Row totalCountRow = sheet.createRow(rowIndex++);
            totalCountRow.createCell(0, CellType.STRING).setCellValue("Total de productos");
            totalCountRow.createCell(1, CellType.NUMERIC).setCellValue(products.size());

            Row totalValueRow = sheet.createRow(rowIndex);
            totalValueRow.createCell(0, CellType.STRING).setCellValue("Valor total de inventario");
            totalValueRow.createCell(1, CellType.NUMERIC).setCellValue(totalValue);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        }
    }

    private static double totalInventoryValue(List<Product> products) {
        return products.stream()
                .mapToDouble(p -> (p.price() != null ? p.price() : 0) * (p.stock() != null ? p.stock() : 0))
                .sum();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static PDPage newPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    private static PDPageContentStream newContentStream(PDDocument document, PDPage page) throws IOException {
        return new PDPageContentStream(document, page);
    }

    private static float writeTitle(PDPageContentStream stream, PDType1Font bold, float y) throws IOException {
        stream.beginText();
        stream.setFont(bold, 16);
        stream.newLineAtOffset(MARGIN, y);
        stream.showText("Reporte de Productos");
        stream.endText();
        return y - (ROW_HEIGHT * 2);
    }

    private static float writeHeaderRow(PDPageContentStream stream, PDType1Font bold, float y) throws IOException {
        for (int i = 0; i < HEADERS.length; i++) {
            drawCell(stream, bold, COLUMN_X[i], y, HEADERS[i]);
        }
        return y - ROW_HEIGHT;
    }

    private static void writeRow(PDPageContentStream stream, PDType1Font font, float y, Product product) throws IOException {
        drawCell(stream, font, COLUMN_X[0], y, product.sku());
        drawCell(stream, font, COLUMN_X[1], y, product.name());
        drawCell(stream, font, COLUMN_X[2], y, nullToEmpty(product.category()));
        drawCell(stream, font, COLUMN_X[3], y, String.format(Locale.US, "%.2f", product.price()));
        drawCell(stream, font, COLUMN_X[4], y, String.valueOf(product.stock()));
        drawCell(stream, font, COLUMN_X[5], y, Boolean.TRUE.equals(product.active()) ? "Sí" : "No");
    }

    private static void writeFooter(PDPageContentStream stream, PDType1Font bold, float y, int count, double totalValue) throws IOException {
        float footerY = y - ROW_HEIGHT;
        stream.beginText();
        stream.setFont(bold, 11);
        stream.newLineAtOffset(MARGIN, footerY);
        stream.showText(String.format(Locale.US, "Total de productos: %d — Valor total de inventario: %.2f", count, totalValue));
        stream.endText();
    }

    private static void drawCell(PDPageContentStream stream, PDType1Font font, float x, float y, String text) throws IOException {
        stream.beginText();
        stream.setFont(font, 9);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitize(text));
        stream.endText();
    }

    // PDFBox's standard fonts (WinAnsi encoding) can't render every Unicode code point a
    // free-text product name/category might contain — replace anything outside the
    // encodable range instead of letting showText() throw mid-report.
    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            builder.append(c < 256 ? c : '?');
        }
        return builder.toString();
    }
}

package com.example.blood_donor.utils;

import android.os.Build;

import com.example.blood_donor.models.exceptions.AnalyticsErrorCode;
import com.example.blood_donor.models.exceptions.AnalyticsException;
import com.example.blood_donor.models.modules.ReportData;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class PDFReportGenerator implements ReportGenerator {
    @Override
    public byte[] generate(ReportData data) throws AnalyticsException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, output);
            document.open();

            // Add header
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            document.add(new Paragraph("Blood Donation Analytics Report", headerFont));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                document.add(new Paragraph("Generated: " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            }
            document.add(new Paragraph("\n"));

            // Add metrics table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 3});

            // Style for headers
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            Font headerCellFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);

            // Add table headers
            addTableHeader(table, headerCellFont, "Metric", "Value");

            // Add data rows
            for (Map.Entry<String, Object> entry : data.getMetrics().entrySet()) {
                addTableRow(table, cellFont, entry.getKey(), formatValue(entry.getValue()));
            }

            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new AnalyticsException(AnalyticsErrorCode.EXPORT_ERROR, e.getMessage());
        }
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, Font font, String key, String value) {
        table.addCell(new PdfPCell(new Phrase(key, font)));
        table.addCell(new PdfPCell(new Phrase(value, font)));
    }

    private String formatValue(Object value) {
        if (value instanceof Map) {
            StringBuilder sb = new StringBuilder();
            ((Map<?, ?>) value).forEach((k, v) ->
                    sb.append(k).append(": ").append(v).append("\n"));
            return sb.toString();
        }
        return value.toString();
    }
}



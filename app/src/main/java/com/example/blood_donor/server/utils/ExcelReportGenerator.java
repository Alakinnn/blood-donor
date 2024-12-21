package com.example.blood_donor.server.utils;

import com.example.blood_donor.server.models.exceptions.AnalyticsErrorCode;
import com.example.blood_donor.server.models.exceptions.AnalyticsException;
import com.example.blood_donor.server.models.modules.ReportData;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Map;

public class ExcelReportGenerator implements ReportGenerator {
    @Override
    public byte[] generate(ReportData data) throws AnalyticsException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Analytics Report");

            // Set fixed column widths (in units of 256)
            sheet.setColumnWidth(0, 8000); // Metric column
            sheet.setColumnWidth(1, 10000); // Value column

            // Add title
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Blood Donation Analytics Report");

            // Add generation time
            Row timeRow = sheet.createRow(1);
            Cell timeCell = timeRow.createCell(0);
            timeCell.setCellValue("Generated: " + LocalDateTime.now().toString());

            // Add headers
            Row headerRow = sheet.createRow(3);
            Cell headerCell1 = headerRow.createCell(0);
            Cell headerCell2 = headerRow.createCell(1);
            headerCell1.setCellValue("Metric");
            headerCell2.setCellValue("Value");

            // Add data
            int rowNum = 4;
            for (Map.Entry<String, Object> entry : data.getMetrics().entrySet()) {
                Row row = sheet.createRow(rowNum++);
                Cell keyCell = row.createCell(0);
                Cell valueCell = row.createCell(1);

                keyCell.setCellValue(entry.getKey());

                Object value = entry.getValue();
                if (value instanceof Map) {
                    // Handle Map values
                    StringBuilder sb = new StringBuilder();
                    ((Map<?, ?>) value).forEach((k, v) ->
                            sb.append(k).append(": ").append(v).append("\n")
                    );
                    valueCell.setCellValue(sb.toString());
                } else {
                    valueCell.setCellValue(value.toString());
                }
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new AnalyticsException(AnalyticsErrorCode.EXPORT_ERROR, e.getMessage());
        }
    }
}
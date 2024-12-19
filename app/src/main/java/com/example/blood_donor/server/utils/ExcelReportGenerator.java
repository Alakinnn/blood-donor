package com.example.blood_donor.server.utils;

import android.os.Build;

import com.example.blood_donor.server.models.exceptions.AnalyticsErrorCode;
import com.example.blood_donor.server.models.exceptions.AnalyticsException;
import com.example.blood_donor.server.models.modules.ReportData;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ExcelReportGenerator implements ReportGenerator {
    @Override
    public byte[] generate(ReportData data) throws AnalyticsException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Analytics Report");

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // Add title and timestamp
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Blood Donation Analytics Report");
            titleCell.setCellStyle(headerStyle);

            Row timestampRow = sheet.createRow(1);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                timestampRow.createCell(0).setCellValue("Generated: " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            // Add headers
            Row headerRow = sheet.createRow(3);
            headerRow.createCell(0).setCellValue("Metric");
            headerRow.createCell(1).setCellValue("Value");
            headerRow.getCell(0).setCellStyle(headerStyle);
            headerRow.getCell(1).setCellStyle(headerStyle);

            // Add data
            int rowNum = 4;
            for (Map.Entry<String, Object> entry : data.getMetrics().entrySet()) {
                Row row = sheet.createRow(rowNum++);
                Cell keyCell = row.createCell(0);
                Cell valueCell = row.createCell(1);

                keyCell.setCellValue(entry.getKey());
                keyCell.setCellStyle(dataStyle);

                setCellValue(valueCell, entry.getValue());
                valueCell.setCellStyle(dataStyle);
            }

            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(output);
            return output.toByteArray();

        } catch (Exception e) {
            throw new AnalyticsException(AnalyticsErrorCode.EXPORT_ERROR, e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void setCellValue(Cell cell, Object value) {
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Map) {
            StringBuilder sb = new StringBuilder();
            ((Map<?, ?>) value).forEach((k, v) ->
                    sb.append(k).append(": ").append(v).append("\n"));
            cell.setCellValue(sb.toString());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
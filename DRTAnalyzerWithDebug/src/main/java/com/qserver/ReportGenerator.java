package com.qserver;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.csv.CSVRecord;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Generates detailed and interactive reports.
 */
public class ReportGenerator {

    /**
     * Generates an Excel report from the analyzed data.
     *
     * @param records The list of CSV records to include in the report.
     * @param outputPath The path to save the report.
     * @throws IOException If an I/O error occurs.
     */
    public static void generateReport(List<CSVRecord> records, String outputPath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Transaction Report");

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Transaction ID", "Start Time", "End Time", "Length", "Waiting Time"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Add data rows
        int rowNum = 1;
        for (CSVRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(record.get("transactionid"));
            row.createCell(1).setCellValue(record.get("starttime"));
            row.createCell(2).setCellValue(record.get("endtime"));
            row.createCell(3).setCellValue(Double.parseDouble(record.get("length")));
            row.createCell(4).setCellValue(Double.parseDouble(record.get("waitingtime")));
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}
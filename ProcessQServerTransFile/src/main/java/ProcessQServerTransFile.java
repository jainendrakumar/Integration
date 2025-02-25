import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProcessQServerTransFile {

    public static void main(String[] args) {
        String folderPath = "C:\\Users\\jkr3\\Desktop\\CRIS\\20250214\\Log\\qserver"; // Replace with the folder path
        String outputFilePath = "Count_LoadPipeline.xlsx"; // Replace with the output file path

        // Step 1: Find the file with the name format QServerTrans*.csv
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.matches("QServerTrans.*\\.csv"));

        if (files == null || files.length == 0) {
            System.out.println("No matching files found.");
            return;
        }

        // Step 2: Extract the date from the filename
        String fileName = files[0].getName();
        String dateString = fileName.replaceAll(".*(\\d{8}_\\d{4}).*", "$1");
        SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd MMM yyyy 'at' HH:mm a");

        Date fileDate = null;
        try {
            fileDate = fileNameFormat.parse(dateString);
        } catch (ParseException e) {
            System.out.println("Error parsing date from filename: " + e.getMessage());
            return;
        }

        System.out.println("File date: " + outputDateFormat.format(fileDate));

        // Step 3: Read the file and process the data
        Map<String, Integer> changesPerMinute = new TreeMap<>(); // Key: Minute, Value: Sum of changes

        try (BufferedReader br = new BufferedReader(new FileReader(files[0]))) {
            String line;
            br.readLine(); // Skip header line

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",");

                String status = fields[5].trim();
                String startTime = fields[10].trim();
                String actionElementName = fields[31].trim();
                String changes = fields[26].trim();

                if (actionElementName.equals("QI_ProcessNVTLoadPipeline") && status.equals("Finished")) {
                    try {
                        int changeCount = Integer.parseInt(changes);
                        String minute = startTime.substring(0, 16); // Extract YYYY-MM-DD HH:MM

                        changesPerMinute.put(minute, changesPerMinute.getOrDefault(minute, 0) + changeCount);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid changes value: " + changes);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            return;
        }

        // Step 4: Write the results to an Excel file
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("LoadPipeline Changes");

            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Minute");
            headerRow.createCell(1).setCellValue("Sum of Changes");

            // Populate data rows
            int rowNum = 1;
            for (Map.Entry<String, Integer> entry : changesPerMinute.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }

            // Write the output to a file
            try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
                workbook.write(fileOut);
                System.out.println("Output written to: " + outputFilePath);
            }
        } catch (IOException e) {
            System.out.println("Error writing Excel file: " + e.getMessage());
        }
    }
}
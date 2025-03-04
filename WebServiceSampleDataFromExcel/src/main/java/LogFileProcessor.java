import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class LogFileProcessor {

    private static final String LOG_FILE_PATH = "E:\\temp\\DRT\\sample_log.csv"; // Path to the log file
    private static final String OUTPUT_EXCEL_PATH = "E:\\temp\\DRT\\DRT_Analysis.xlsx"; // Output Excel file path

    public static void main(String[] args) {
        try {
            // Parse the log file and extract relevant information
            List<String[]> errorRows = new ArrayList<>();
            parseLogFile(Paths.get(LOG_FILE_PATH), errorRows);

            // Write the extracted data to an Excel file
            writeToExcel(OUTPUT_EXCEL_PATH, errorRows);

            System.out.println("Processing completed. Results saved to: " + OUTPUT_EXCEL_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseLogFile(Path logFilePath, List<String[]> errorRows) {
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath.toFile()))) {
            String headerLine = reader.readLine(); // Read header
            if (headerLine == null || !headerLine.contains("index")) {
                throw new IllegalArgumentException("Invalid log file format.");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERRORCODE") || line.contains("STRUCTUREDEXCEPTION") || line.contains("CRASH")) {
                    String[] parts = line.split(",", -1); // Split by comma, retain empty values
                    if (parts.length >= 16) {
                        String startTime = parts[1].replaceAll("\"", ""); // Remove quotes
                        String threadName = parts[7].replaceAll("\"", "");
                        String error = line.contains("ERRORCODE") ? "ERRORCODE" :
                                line.contains("STRUCTUREDEXCEPTION") ? "STRUCTUREDEXCEPTION" :
                                        "CRASH";
                        String logKind = parts[11].replaceAll("\"", "");
                        String description = parts[12].replaceAll("\"", "");
                        String mds = parts[15].replaceAll("\"", "");

                        // Add relevant data to the list
                        errorRows.add(new String[]{startTime, threadName, error, logKind, description, mds});
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading log file: " + logFilePath + " - " + e.getMessage());
        }
    }

    private static void writeToExcel(String outputPath, List<String[]> errorRows) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Error");

        // Write header row
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Start Time");
        header.createCell(1).setCellValue("Thread Name");
        header.createCell(2).setCellValue("Error");
        header.createCell(3).setCellValue("Log Kind");
        header.createCell(4).setCellValue("Description");
        header.createCell(5).setCellValue("MDS");

        // Write data rows
        int rowIndex = 1;
        for (String[] row : errorRows) {
            Row dataRow = sheet.createRow(rowIndex++);
            for (int colIndex = 0; colIndex < row.length; colIndex++) {
                dataRow.createCell(colIndex).setCellValue(row[colIndex]);
            }
        }

        // Save the Excel file
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }

        workbook.close();
    }
}

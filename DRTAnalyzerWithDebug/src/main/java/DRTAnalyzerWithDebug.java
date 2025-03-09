import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Jainendra Kumar
 * TODO:
 *
 * @author JKR3
 */

public class DRTAnalyzerWithDebug {
    private static final String ZIP_FILE_PATH = "E:\\temp\\DRT\\38.zip"; // Replace with your ZIP file path
    private static final String EXTRACTION_PATH = "E:\\temp\\DRT\\extracted\\"; // Extraction directory
    private static final String OUTPUT_EXCEL_PATH = "E:\\temp\\DRT\\DRT_Analysis.xlsx"; // Excel output file path

    public static void main(String[] args) {
        try {
            // Step 1: Unzip the file
            debug("Starting to unzip the file...");
            unzipFile(ZIP_FILE_PATH, EXTRACTION_PATH);

            // Step 2-5: Parse and process files
            debug("Parsing and analyzing files...");
            List<String[]> errorRows = new ArrayList<>();
            parseFiles(Paths.get(EXTRACTION_PATH), errorRows);

            // Step 6: Write to Excel
            debug("Writing extracted data to Excel...");
            writeToExcel(OUTPUT_EXCEL_PATH, errorRows);

            debug("Processing completed successfully. Excel file created at: " + OUTPUT_EXCEL_PATH);
        } catch (Exception e) {
            error("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void unzipFile(String zipFilePath, String extractionPath) throws ZipException {
        File destDir = new File(extractionPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        ZipFile zipFile = new ZipFile(zipFilePath);
        zipFile.extractAll(extractionPath);

        debug("Unzipped file to directory: " + extractionPath);
    }

    private static void parseFiles(Path directory, List<String[]> errorRows) throws IOException {
        Files.walk(directory)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".csv") && path.toString().contains("qserver"))
                .forEach(file -> processCsvFile(file, errorRows));
    }

    private static void processCsvFile(Path file, List<String[]> errorRows) {
        debug("Processing file: " + file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null || !headerLine.contains("index")) {
                debug("Skipping file due to invalid format: " + file);
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERRORCODE") || line.contains("STRUCTUREDEXCEPTION") || line.contains("CRASH")) {
                    String[] parts = line.split(",", -1); // Split the line by comma
                    if (parts.length >= 16) {
                        String startTime = parts[1].replaceAll("\"", ""); // Remove quotes
                        String threadName = parts[7].replaceAll("\"", "");
                        String error = line.contains("ERRORCODE") ? "ERRORCODE" :
                                line.contains("STRUCTUREDEXCEPTION") ? "STRUCTUREDEXCEPTION" : "CRASH";
                        String logKind = parts[11].replaceAll("\"", "");
                        String description = parts[12].replaceAll("\"", "");
                        String mds = parts[15].replaceAll("\"", "");

                        errorRows.add(new String[]{startTime, threadName, error, logKind, description, mds});
                        debug("Captured line: " + String.join(", ", parts));
                    }
                }
            }
        } catch (IOException e) {
            error("Error reading file: " + file + " - " + e.getMessage());
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
        debug("Excel file written successfully to: " + outputPath);
    }

    private static void debug(String message) {
        System.out.println("[DEBUG] " + message);
    }

    private static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
}

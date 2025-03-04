import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DRTLogAnalyzer {
    private static final String ZIP_FILE_PATH = "E:\\temp\\DRT\\38.zip"; // Replace with actual ZIP file path
    private static final String EXTRACTION_PATH = "E:\\temp\\DRT\\Unzip\\"; // Extraction directory
    private static final String OUTPUT_EXCEL_PATH = "E:\\temp\\DRT\\DRT_Analysis.xlsx"; // Excel output file path
    private static final String PASSWORD = "your_password"; // Replace with the actual password

    public static void main(String[] args) {
        try {
            // Step 1: Unzip the file
            System.out.println("in main: ");
            unzipFile(ZIP_FILE_PATH, EXTRACTION_PATH);
            System.out.println("unzip done ");


            // Step 2-5: Parse the files and extract required information
            List<String[]> errorRows = new ArrayList<>();
            parseQServerFiles(Paths.get(EXTRACTION_PATH), errorRows);

            // Step 6: Write to Excel
            writeToExcel(OUTPUT_EXCEL_PATH, errorRows);

            System.out.println("Analysis completed. Excel file created at: " + OUTPUT_EXCEL_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void unzipFile(String zipFilePath, String extractionPath) throws ZipException {
        System.out.println("ZIP file to start: " );

        ZipFile zipFile = new ZipFile(zipFilePath);
        System.out.println("ZIP file reading: ");

        if (zipFile.isEncrypted()) {
            System.out.println("ZIP file is encrypted " );

            zipFile.setPassword(PASSWORD.toCharArray());
            System.out.println("ZIP file password : ");

        }
        System.out.println("ZIP file start extracted to: " + extractionPath);

        zipFile.extractAll(extractionPath);
        System.out.println("ZIP file extracted to: " + extractionPath);
    }

    private static void parseQServerFiles(Path directory, List<String[]> errorRows) throws IOException {
        Files.walk(directory)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".csv") && path.toString().contains("qserver"))
                .forEach(file -> processCsvFile(file, errorRows));
    }

    private static void processCsvFile(Path file, List<String[]> errorRows) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERRORCODE") || line.contains("STRUCTUREDEXCEPTION") || line.contains("CRASH")) {
                    String[] parts = line.split(",", -1); // Split the CSV line
                    if (parts.length >= 16) {
                        String startTime = parts[1];
                        String threadName = parts[7];
                        String error = line.contains("ERRORCODE") ? "ERRORCODE" :
                                line.contains("STRUCTUREDEXCEPTION") ? "STRUCTUREDEXCEPTION" :
                                        "CRASH";
                        String logKind = parts[11];
                        String description = parts[12];
                        String mds = parts[15];

                        // Add relevant information to errorRows
                        errorRows.add(new String[]{startTime, threadName, error, logKind, description, mds});
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + file + " - " + e.getMessage());
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

        // Save Excel file
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }

        workbook.close();
    }
}

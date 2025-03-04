import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class DRTAnalyzer {

    private static final String ZIP_FILE_PATH = "E:\\temp\\DRT\\38.zip"; // Your ZIP file path
    private static final String EXTRACTION_PATH = "E:\\temp\\DRT\\extracted\\"; // Extraction directory
    private static final String OUTPUT_EXCEL_PATH = "E:\\temp\\DRT\\DRT_Analysis.xlsx"; // Excel output file path

    public static void main(String[] args) {
        try {
            // Step 1: Unzip the file
            debug("Starting to unzip the file...");
            unzipFile(ZIP_FILE_PATH, EXTRACTION_PATH);

            // Step 2: Count files in each folder
            debug("Counting files in folders...");
            Map<String, Long> folderFileCounts = countFilesInFolders(Paths.get(EXTRACTION_PATH));

            // Step 3-6: Parse and analyze CSV files
            debug("Parsing and analyzing CSV files...");
            List<String[]> errorRows = new ArrayList<>();
            parseCsvFiles(Paths.get(EXTRACTION_PATH), errorRows);

            // Write results to Excel
            debug("Writing results to Excel...");
            writeToExcel(OUTPUT_EXCEL_PATH, folderFileCounts, errorRows);

            debug("Processing completed successfully. Results saved to: " + OUTPUT_EXCEL_PATH);
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

    private static Map<String, Long> countFilesInFolders(Path directory) throws IOException {
        debug("Counting files in subfolders...");
        Map<String, Long> folderFileCounts = Files.walk(directory)
                .filter(Files::isDirectory)
                .collect(Collectors.toMap(
                        Path::toString,
                        path -> {
                            try {
                                return Files.list(path).filter(Files::isRegularFile).count();
                            } catch (IOException e) {
                                error("Error counting files in folder: " + path);
                                return 0L;
                            }
                        }
                ));

        folderFileCounts.forEach((folder, count) -> debug("Folder: " + folder + ", File Count: " + count));
        return folderFileCounts;
    }

    private static void parseCsvFiles(Path directory, List<String[]> errorRows) throws IOException {
        Files.walk(directory)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".csv") && path.toString().contains("qserver"))
                .forEach(file -> processCsvFile(file, errorRows));
    }

    private static void processCsvFile(Path file, List<String[]> errorRows) {
        debug("Processing file: " + file);
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String headerLine = reader.readLine(); // Read header
            if (headerLine == null || !headerLine.contains("index")) {
                debug("Skipping file due to invalid format: " + file);
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ERRORCODE") || line.contains("STRUCTUREDEXCEPTION") || line.contains("CRASH")) {
                    String[] parts = line.split(",", -1); // Split by comma
                    if (parts.length >= 16) {
                        String filename = file.getFileName().toString();
                        String index = parts[0];
                        String startTime = parts[1].replaceAll("\"", ""); // Remove quotes
                        String threadName = parts[7].replaceAll("\"", "");
                        String error = line.contains("ERRORCODE") ? "ERRORCODE" :
                                line.contains("STRUCTUREDEXCEPTION") ? "STRUCTUREDEXCEPTION" : "CRASH";
                        String logKind = parts[11].replaceAll("\"", "");
                        String description = parts[12].replaceAll("\"", "");
                        String mds = parts[15].replaceAll("\"", "");

                        errorRows.add(new String[]{filename, index, startTime, threadName, error, logKind, description, mds});
                        debug("Captured line: " + String.join(", ", parts));
                    }
                }
            }
        } catch (IOException e) {
            error("Error reading file: " + file + " - " + e.getMessage());
        }
    }

    private static void writeToExcel(String outputPath, Map<String, Long> folderFileCounts, List<String[]> errorRows) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet errorSheet = workbook.createSheet("Error");
        Sheet folderSheet = workbook.createSheet("Folder File Counts");

        // Write folder file counts
        Row folderHeader = folderSheet.createRow(0);
        folderHeader.createCell(0).setCellValue("Folder Name");
        folderHeader.createCell(1).setCellValue("File Count");

        int folderRowIndex = 1;
        for (Map.Entry<String, Long> entry : folderFileCounts.entrySet()) {
            Row row = folderSheet.createRow(folderRowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        // Write error rows
        Row errorHeader = errorSheet.createRow(0);
        errorHeader.createCell(0).setCellValue("Filename");
        errorHeader.createCell(1).setCellValue("Index");
        errorHeader.createCell(2).setCellValue("Start Time");
        errorHeader.createCell(3).setCellValue("Thread Name");
        errorHeader.createCell(4).setCellValue("Error");
        errorHeader.createCell(5).setCellValue("Log Kind");
        errorHeader.createCell(6).setCellValue("Description");
        errorHeader.createCell(7).setCellValue("MDS");

        int errorRowIndex = 1;
        for (String[] row : errorRows) {
            Row dataRow = errorSheet.createRow(errorRowIndex++);
            for (int colIndex = 0; colIndex < row.length; colIndex++) {
                dataRow.createCell(colIndex).setCellValue(row[colIndex]);
            }
        }

        // Save Excel file
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

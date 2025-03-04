import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class DRTAnalyzerV2 {
    private static final String ZIP_FILE_PATH = "E:\\temp\\DRT\\38.zip"; // Replace with the ZIP file path
    private static final String EXTRACTION_PATH = "E:\\temp\\DRT\\extracted\\"; // Extraction directory
    private static final String OUTPUT_EXCEL_PATH = "E:\\temp\\DRT\\DRT_Analysis.xlsx"; // Excel output file path

    public static void main(String[] args) {
        try {
            debug("Starting analysis...");
            unzipFile(ZIP_FILE_PATH, EXTRACTION_PATH);

            Workbook workbook = new XSSFWorkbook();

            // Step 3: Capture model file names from "Archieved projects folder"
            debug("Capturing model file names...");
            List<String> modelFiles = listFilesInFolder(Paths.get(EXTRACTION_PATH, "Archived Projects"));
            writeInfoSheet(workbook, "Model Files", "Model File Names", modelFiles);

            // Step 4: Capture OS and model details from "META-INF" folder
            debug("Capturing META-INF information...");
            Map<String, String> metaInfDetails = extractMetaInfDetails(Paths.get(EXTRACTION_PATH, "META-INF"));
            writeInfoSheet(workbook, "META-INF Info", "Key", "Value", metaInfDetails);

            // Step 5: Capture number of files in each folder
            debug("Counting files in folders...");
            Map<String, Long> folderFileCounts = countFilesInFolders(Paths.get(EXTRACTION_PATH));
            // Convert Map<String, Long> to Map<String, String>
            Map<String, String> folderFileCountsAsString = folderFileCounts.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> String.valueOf(entry.getValue()) // Convert Long to String
                    ));
            writeInfoSheet(workbook, "Folder File Counts", "Folder", "File Count", folderFileCountsAsString);

            // Step 6-8: Capture ERRORCODE, STRUCTUREDEXCEPTION, and CRASH
            debug("Analyzing CSV files for errors...");
            List<String[]> errorRows = parseErrorRows(Paths.get(EXTRACTION_PATH), Arrays.asList("ERRORCODE", "STRUCTUREDEXCEPTION", "CRASH"));
            writeErrorSheet(workbook, "Error", errorRows);

            // Step 9: Check for dump files
            debug("Checking for dump files...");
            List<String> dumpFiles = listFilesByExtension(Paths.get(EXTRACTION_PATH), ".dmp");
            if (!dumpFiles.isEmpty()) {
                writeInfoSheet(workbook, "Dump Files", "File Name", dumpFiles);
            }

            // Step 10: Process QServerTrans CSV files for transactions
            debug("Processing QServerTrans files...");
            List<String[]> transactions = parseAndConsolidateTransactionsByJobId(Paths.get(EXTRACTION_PATH), "QServerTrans");
            writeTransactionSheet(workbook, "Transaction", transactions);



            // Save the workbook
            try (FileOutputStream fos = new FileOutputStream(OUTPUT_EXCEL_PATH)) {
                workbook.write(fos);
            }
            workbook.close();

            debug("Analysis completed successfully. Results saved at: " + OUTPUT_EXCEL_PATH);
        } catch (Exception e) {
            error("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void unzipFile(String zipFilePath, String extractionPath) throws ZipException {
        ZipFile zipFile = new ZipFile(zipFilePath);
        zipFile.extractAll(extractionPath);
        debug("Unzipped file to: " + extractionPath);
    }

    private static List<String> listFilesInFolder(Path folderPath) throws IOException {
        return Files.walk(folderPath)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
    }

    private static Map<String, String> extractMetaInfDetails(Path metaInfFolder) throws IOException {
        Map<String, String> details = new LinkedHashMap<>();
        Files.walk(metaInfFolder)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("OS") || line.startsWith("Model")) {
                                String[] parts = line.split(":", 2);
                                if (parts.length == 2) {
                                    details.put(parts[0].trim(), parts[1].trim());
                                }
                            }
                        }
                    } catch (IOException e) {
                        error("Error reading META-INF file: " + file + " - " + e.getMessage());
                    }
                });
        return details;
    }

    private static Map<String, Long> countFilesInFolders(Path rootFolder) throws IOException {
        return Files.walk(rootFolder)
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
    }

    private static List<String[]> parseErrorRows(Path rootFolder, List<String> keywords) throws IOException {
        List<String[]> rows = new ArrayList<>();
        Files.walk(rootFolder)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".csv") && path.toString().contains("qserver"))
                .forEach(file -> {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            for (String keyword : keywords) {
                                if (line.contains(keyword)) {
                                    rows.add(parseErrorLine(file.getFileName().toString(), line));
                                    break;
                                }
                            }
                        }
                    } catch (IOException e) {
                        error("Error reading file: " + file + " - " + e.getMessage());
                    }
                });
        return rows;
    }

    private static List<String[]> parseTransactions(Path rootFolder, String filePrefix) throws IOException {
        List<String[]> transactions = new ArrayList<>();
        Map<String, String[]> transactionMap = new HashMap<>();

        Files.walk(rootFolder)
                .filter(path -> Files.isRegularFile(path)
                        && path.toString().endsWith(".csv")
                        && path.getFileName().toString().startsWith(filePrefix))
                .forEach(file -> {
                    debug("Processing transaction file: " + file);
                    try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
                        String headerLine = reader.readLine(); // Read and skip the header
                        if (headerLine == null || !headerLine.contains("transactionid")) {
                            debug("Skipping file due to invalid format: " + file);
                            return;
                        }

                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(",", -1); // Split by commas

                            // Validate row length
                            if (parts.length < 47) {
                                debug("Skipping incomplete row in file " + file + ": " + line);
                                continue;
                            }

                            String transactionId = parts[8]; // Transaction ID
                            String status = parts[5]; // Status (Started/Finished)

                            String[] transactionData = new String[47];
                            for (int i = 0; i < transactionData.length; i++) {
                                transactionData[i] = i < parts.length ? parts[i] : ""; // Handle missing columns
                            }

                            if ("Started".equalsIgnoreCase(status)) {
                                transactionMap.put(transactionId, transactionData);
                            } else if ("Finished".equalsIgnoreCase(status)) {
                                if (transactionMap.containsKey(transactionId)) {
                                    String[] startedData = transactionMap.get(transactionId);
                                    transactions.add(mergeTransactionData(startedData, transactionData));
                                    transactionMap.remove(transactionId);
                                }
                            }
                        }
                    } catch (IOException e) {
                        error("Error reading file: " + file + " - " + e.getMessage());
                    }
                });

        return transactions;
    }

    private static List<String[]> parseAndConsolidateTransactionsByJobId(Path rootFolder, String filePrefix) throws IOException {
        List<String[]> consolidatedTransactions = new ArrayList<>();
        Map<String, String[]> startedJobs = new HashMap<>();

        Files.walk(rootFolder)
                .filter(path -> Files.isRegularFile(path)
                        && path.toString().endsWith(".csv")
                        && path.getFileName().toString().startsWith(filePrefix))
                .forEach(file -> {
                    debug("Processing QServerTrans file: " + file);
                    try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
                        String headerLine = reader.readLine(); // Read and skip the header
                        if (headerLine == null || !headerLine.contains("jobid")) {
                            debug("Skipping file due to invalid format: " + file);
                            return;
                        }

                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(",", -1); // Split by commas

                            // Ensure sufficient columns for parsing
                            if (parts.length < 47) {
                                debug("Skipping incomplete row in file " + file + ": " + line);
                                continue;
                            }

                            String jobId = parts[1]; // Job ID
                            String status = parts[5]; // Status (Started/Finished)

                            if ("Started".equalsIgnoreCase(status)) {
                                startedJobs.put(jobId, parts);
                            } else if ("Finished".equalsIgnoreCase(status)) {
                                if (startedJobs.containsKey(jobId)) {
                                    String[] startedParts = startedJobs.remove(jobId);

                                    // Consolidate data from both lines
                                    String[] consolidatedRow = new String[30];
                                    consolidatedRow[0] = file.getFileName().toString(); // Filename
                                    consolidatedRow[1] = startedParts[0]; // Index (from Started row)
                                    consolidatedRow[2] = startedParts[12]; // Start Time
                                    consolidatedRow[3] = parts[13]; // Finish Time (from Finished row)
                                    consolidatedRow[4] = parts[14]; // Length
                                    consolidatedRow[5] = parts[15]; // Waiting Time
                                    consolidatedRow[6] = parts[16]; // Process Time
                                    consolidatedRow[7] = parts[17]; // Function Time
                                    consolidatedRow[8] = parts[18]; // Delta Set Finalize
                                    consolidatedRow[9] = parts[19]; // IO Def
                                    consolidatedRow[10] = parts[21]; // DB Time
                                    consolidatedRow[11] = parts[22]; // Stream Time
                                    consolidatedRow[12] = parts[23]; // NR Datasets
                                    consolidatedRow[13] = parts[24]; // Size
                                    consolidatedRow[14] = parts[25]; // Constructions
                                    consolidatedRow[15] = parts[26]; // Destructions
                                    consolidatedRow[16] = parts[27]; // Changes
                                    consolidatedRow[17] = parts[34]; // Description
                                    consolidatedRow[18] = parts[35]; // Message ID
                                    consolidatedRow[19] = parts[36]; // Lock Profile
                                    consolidatedRow[20] = parts[37]; // Proc Mem
                                    consolidatedRow[21] = parts[38]; // Func Mem
                                    consolidatedRow[22] = parts[39]; // DB Mem
                                    consolidatedRow[23] = parts[40]; // Stream Mem
                                    consolidatedRow[24] = parts[41]; // OS VM Size
                                    consolidatedRow[25] = parts[42]; // Free Memory
                                    consolidatedRow[26] = parts[43]; // Change IDs
                                    consolidatedRow[27] = parts[44]; // Transaction Reason ID
                                    consolidatedRow[28] = parts[45]; // Transaction Reason

                                    consolidatedTransactions.add(consolidatedRow);
                                }
                            }
                        }
                    } catch (IOException e) {
                        error("Error reading file: " + file + " - " + e.getMessage());
                    }
                });

        return consolidatedTransactions;
    }

    private static void writeTransactionSheet(Workbook workbook, String sheetName, List<String[]> transactions) {
        if (transactions.isEmpty()) {
            debug("No transaction data to write.");
            return;
        }

        Sheet sheet = workbook.createSheet(sheetName);

        // Define headers
        String[] headers = {
                "Filename", "Index", "Start Time", "Finish Time", "Length", "Waiting Time",
                "Process Time", "Function Time", "Delta Set Finalize", "IO Def", "DB Time",
                "Stream Time", "NR Datasets", "Size", "Constructions", "Destructions",
                "Changes", "Description", "Message ID", "Lock Profile", "Proc Mem",
                "Func Mem", "DB Mem", "Stream Mem", "OS VM Size", "Free Memory",
                "Change IDs", "Transaction Reason ID", "Transaction Reason"
        };

        // Write headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Write data rows
        int rowIndex = 1;
        for (String[] transaction : transactions) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < transaction.length; i++) {
                row.createCell(i).setCellValue(transaction[i]);
            }
        }

        debug("Transaction data written to sheet: " + sheetName);
    }


    private static List<String[]> parseAndConsolidateTransactions(Path rootFolder, String filePrefix) throws IOException {
        List<String[]> consolidatedTransactions = new ArrayList<>();
        Map<String, String[]> startedTransactions = new HashMap<>();

        Files.walk(rootFolder)
                .filter(path -> Files.isRegularFile(path)
                        && path.toString().endsWith(".csv")
                        && path.getFileName().toString().startsWith(filePrefix))
                .forEach(file -> {
                    debug("Processing QServerTrans file: " + file);
                    try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
                        String headerLine = reader.readLine(); // Read and skip the header
                        if (headerLine == null || !headerLine.contains("transactionid")) {
                            debug("Skipping file due to invalid format: " + file);
                            return;
                        }

                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(",", -1); // Split by commas

                            // Ensure sufficient columns for parsing
                            if (parts.length < 47) {
                                debug("Skipping incomplete row in file " + file + ": " + line);
                                continue;
                            }

                            String transactionId = parts[8]; // Transaction ID
                            String status = parts[5]; // Status (Started/Finished)

                            if ("Started".equalsIgnoreCase(status)) {
                                startedTransactions.put(transactionId, parts);
                            } else if ("Finished".equalsIgnoreCase(status)) {
                                if (startedTransactions.containsKey(transactionId)) {
                                    String[] startedParts = startedTransactions.remove(transactionId);

                                    // Consolidate data from both lines
                                    String[] consolidatedRow = new String[30];
                                    consolidatedRow[0] = file.getFileName().toString(); // Filename
                                    consolidatedRow[1] = startedParts[0]; // Index (from Started row)
                                    consolidatedRow[2] = startedParts[13]; // Start Time
                                    consolidatedRow[3] = parts[14]; // Finish Time (from Finished row)
                                    consolidatedRow[4] = parts[15]; // Length
                                    consolidatedRow[5] = parts[16]; // Waiting Time
                                    consolidatedRow[6] = parts[17]; // Process Time
                                    consolidatedRow[7] = parts[18]; // Function Time
                                    consolidatedRow[8] = parts[19]; // Delta Set Finalize
                                    consolidatedRow[9] = parts[20]; // IO Def
                                    consolidatedRow[10] = parts[22]; // DB Time
                                    consolidatedRow[11] = parts[23]; // Stream Time
                                    consolidatedRow[12] = parts[24]; // NR Datasets
                                    consolidatedRow[13] = parts[25]; // Size
                                    consolidatedRow[14] = parts[26]; // Constructions
                                    consolidatedRow[15] = parts[27]; // Destructions
                                    consolidatedRow[16] = parts[28]; // Changes
                                    consolidatedRow[17] = parts[35]; // Description
                                    consolidatedRow[18] = parts[36]; // Message ID
                                    consolidatedRow[19] = parts[37]; // Lock Profile
                                    consolidatedRow[20] = parts[38]; // Proc Mem
                                    consolidatedRow[21] = parts[39]; // Func Mem
                                    consolidatedRow[22] = parts[40]; // DB Mem
                                    consolidatedRow[23] = parts[41]; // Stream Mem
                                    consolidatedRow[24] = parts[42]; // OS VM Size
                                    consolidatedRow[25] = parts[43]; // Free Memory
                                    consolidatedRow[26] = parts[44]; // Change IDs
                                    consolidatedRow[27] = parts[45]; // Transaction Reason ID
                                    consolidatedRow[28] = parts[46]; // Transaction Reason

                                    consolidatedTransactions.add(consolidatedRow);
                                }
                            }
                        }
                    } catch (IOException e) {
                        error("Error reading file: " + file + " - " + e.getMessage());
                    }
                });

        return consolidatedTransactions;
    }

    private static void writeTransactionSheet2(Workbook workbook, String sheetName, List<String[]> transactions) {
        if (transactions.isEmpty()) {
            debug("No transaction data to write.");
            return;
        }

        Sheet sheet = workbook.createSheet(sheetName);

        // Define headers
        String[] headers = {
                "Filename", "Index", "Start Time", "Finish Time", "Length", "Waiting Time",
                "Process Time", "Function Time", "Delta Set Finalize", "IO Def", "DB Time",
                "Stream Time", "NR Datasets", "Size", "Constructions", "Destructions",
                "Changes", "Description", "Message ID", "Lock Profile", "Proc Mem",
                "Func Mem", "DB Mem", "Stream Mem", "OS VM Size", "Free Memory",
                "Change IDs", "Transaction Reason ID", "Transaction Reason"
        };

        // Write headers
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Write data rows
        int rowIndex = 1;
        for (String[] transaction : transactions) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < transaction.length; i++) {
                row.createCell(i).setCellValue(transaction[i]);
            }
        }

        debug("Transaction data written to sheet: " + sheetName);
    }


    private static String[] mergeTransactionData(String[] startedData, String[] finishedData) {
        String[] merged = new String[startedData.length];
        System.arraycopy(startedData, 0, merged, 0, startedData.length);

        // Replace the "Finish Time" and other details with data from the finished row
        merged[4] = finishedData[4]; // Finish Time
        merged[5] = finishedData[5]; // Length
        merged[6] = finishedData[6]; // Waiting Time
        merged[7] = finishedData[7]; // Process Time
        merged[8] = finishedData[8]; // Function Time
        merged[9] = finishedData[9]; // Delta Set Finalize
        merged[10] = finishedData[10]; // DB Time
        merged[11] = finishedData[11]; // Stream Time
        merged[12] = finishedData[12]; // NR Datasets
        merged[13] = finishedData[13]; // Size
        merged[14] = finishedData[14]; // Constructions
        merged[15] = finishedData[15]; // Destructions
        merged[16] = finishedData[16]; // Changes
        merged[17] = finishedData[17]; // Description
        merged[18] = finishedData[18]; // Message ID
        merged[19] = finishedData[19]; // Lock Profile
        merged[20] = finishedData[20]; // Proc Mem
        merged[21] = finishedData[21]; // Func Mem
        merged[22] = finishedData[22]; // DB Mem
        merged[23] = finishedData[23]; // Stream Mem
        merged[24] = finishedData[24]; // OS VM Size
        merged[25] = finishedData[25]; // Free Memory
        merged[26] = finishedData[26]; // Change IDs
        merged[27] = finishedData[27]; // Transaction Reason ID
        merged[28] = finishedData[28]; // Transaction Reason

        return merged;
    }


    private static List<String> listFilesByExtension(Path rootFolder, String extension) throws IOException {
        return Files.walk(rootFolder)
                .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(extension))
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static void writeInfoSheet(Workbook workbook, String sheetName, String keyColumn, String valueColumn, Map<String, String> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue(keyColumn);
        header.createCell(1).setCellValue(valueColumn);
        int rowIndex = 1;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
    }

    private static void writeInfoSheet(Workbook workbook, String sheetName, String header, List<String> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue(header);
        int rowIndex = 1;
        for (String value : data) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(value);
        }
    }

    private static void writeErrorSheet(Workbook workbook, String sheetName, List<String[]> rows) {
        Sheet sheet = workbook.createSheet(sheetName);
        String[] headers = {"Filename", "Index", "Start Time", "Thread Name", "Error", "Log Kind", "Description", "MDS"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        int rowIndex = 1;
        for (String[] row : rows) {
            Row dataRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < row.length; i++) {
                dataRow.createCell(i).setCellValue(row[i]);
            }
        }
    }

    private static void writeTransactionSheet1(Workbook workbook, String sheetName, List<String[]> transactions) {
        if (transactions.isEmpty()) {
            debug("No transaction data to write.");
            return;
        }

        Sheet sheet = workbook.createSheet(sheetName);
        String[] headers = {
                "Filename", "Index", "Start Time", "Status", "Finish Time", "Length", "Waiting Time",
                "Process Time", "Function Time", "Delta Set Finalize", "DB Time", "Stream Time",
                "NR Datasets", "Size", "Constructions", "Destructions", "Changes", "Description",
                "Message ID", "Lock Profile", "Proc Mem", "Func Mem", "DB Mem", "Stream Mem",
                "OS VM Size", "Free Memory", "Change IDs", "Transaction Reason ID", "Transaction Reason"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        int rowIndex = 1;
        for (String[] transaction : transactions) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < transaction.length; i++) {
                row.createCell(i).setCellValue(transaction[i]);
            }
        }

        debug("Transaction data written to sheet: " + sheetName);
    }


    private static String[] parseErrorLine(String filename, String line) {
        // Your logic to parse a line with ERRORCODE, STRUCTUREDEXCEPTION, or CRASH
        return new String[]{filename, "Index", "Start Time", "Thread Name", "Error", "Log Kind", "Description", "MDS"};
    }

    private static void debug(String message) {
        System.out.println("[DEBUG] " + message);
    }

    private static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
}

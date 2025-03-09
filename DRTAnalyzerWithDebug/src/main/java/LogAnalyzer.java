import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import org.apache.commons.csv.*;

/**
 * Jainendra Kumar
 * TODO:
 *
 * @author JKR3
 */

public class LogAnalyzer {

    public static void main(String[] args) {
        // Load properties from config.properties
        Properties properties = loadProperties("config.properties");
        // Get the log directory; default to "logs" if not defined
        String logDirectory = properties.getProperty("log.directory", "logs");

        // Lists to aggregate records based on log type
        List<CSVRecord> transactionRecords = new ArrayList<>();
        List<CSVRecord> jobRecords = new ArrayList<>();
        List<CSVRecord> componentRecords = new ArrayList<>();

        try {
            List<Path> csvFiles = getCsvFiles(logDirectory);
            System.out.println("Found " + csvFiles.size() + " CSV files in directory: " + logDirectory);
            // Process each CSV file
            for (Path file : csvFiles) {
                processCsvFile(file, transactionRecords, jobRecords, componentRecords);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV files: " + e.getMessage());
        }

        // Perform analysis based on file type
        if (!transactionRecords.isEmpty()) {
            analyzeTransactions(transactionRecords);
        }
        if (!jobRecords.isEmpty()) {
            analyzeJobs(jobRecords);
        }
        if (!componentRecords.isEmpty()) {
            analyzeComponentLogs(componentRecords);
        }
    }

    // Method to load properties from a file
    private static Properties loadProperties(String filename) {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException ex) {
            System.err.println("Error loading properties file: " + ex.getMessage());
        }
        return properties;
    }

    // Recursively obtain CSV files from the given directory
    public static List<Path> getCsvFiles(String directory) throws IOException {
        List<Path> csvFiles = new ArrayList<>();
        Files.walk(Paths.get(directory))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                .forEach(csvFiles::add);
        return csvFiles;
    }

    // Process each CSV file: detect its type based on headers and aggregate its records
    public static void processCsvFile(Path file, List<CSVRecord> transactionRecords,
                                      List<CSVRecord> jobRecords, List<CSVRecord> componentRecords) {
        try (Reader reader = new FileReader(file.toFile());
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();

            // Determine log type based on available columns
            boolean isTransaction = headerMap.containsKey("transactionid");
            boolean isJob = headerMap.containsKey("jobid") && (headerMap.containsKey("posted") || headerMap.containsKey("started"));
            boolean isComponent = headerMap.containsKey("loglevel") || headerMap.containsKey("process");

            // Add each record to the corresponding list
            for (CSVRecord record : parser) {
                if (isTransaction) {
                    transactionRecords.add(record);
                } else if (isJob) {
                    jobRecords.add(record);
                } else if (isComponent) {
                    componentRecords.add(record);
                }
            }

            System.out.println("Processed file: " + file.getFileName() + " as " +
                    (isTransaction ? "Transaction" : isJob ? "Job" : isComponent ? "Component" : "Unknown"));

        } catch (IOException e) {
            System.err.println("Error processing file " + file.getFileName() + ": " + e.getMessage());
        }
    }

    // Analyze transaction logs: compute execution time and waiting time summaries
    public static void analyzeTransactions(List<CSVRecord> records) {
        System.out.println("\n--- Transaction Log Analysis ---");
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;  // Assumes ISO 8601 format
        List<Double> executionTimes = new ArrayList<>();

        // Inside analyzeTransactions() method:
        for (CSVRecord record : records) {
            try {
                String startStr = record.get("starttime");
                String endStr = record.get("endtime");
                if (startStr != null && endStr != null && !startStr.isEmpty() && !endStr.isEmpty()) {
                    LocalDateTime startTime = LogAnalyzerUtil.parseDateTime(startStr);
                    LocalDateTime endTime = LogAnalyzerUtil.parseDateTime(endStr);
                    Duration duration = Duration.between(startTime, endTime);
                    double seconds = duration.toMillis() / 1000.0;
                    executionTimes.add(seconds);
                }
            } catch (Exception e) {
                System.err.println("Error parsing dates in transaction record: " + e.getMessage());
            }
        }


        if (!executionTimes.isEmpty()) {
            double sum = executionTimes.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / executionTimes.size();
            double min = executionTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = executionTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            System.out.println("Execution Time Summary:");
            System.out.println("Count: " + executionTimes.size());
            System.out.println("Average: " + avg + " seconds");
            System.out.println("Min: " + min + " seconds");
            System.out.println("Max: " + max + " seconds");
        }

        // Waiting time analysis (if available)
        List<Double> waitingTimes = new ArrayList<>();
        for (CSVRecord record : records) {
            try {
                String waitingStr = record.get("waitingtime");
                if (waitingStr != null && !waitingStr.isEmpty()) {
                    double wt = Double.parseDouble(waitingStr);
                    waitingTimes.add(wt);
                }
            } catch (Exception e) {
                System.err.println("Error parsing waiting time: " + e.getMessage());
            }
        }
        if (!waitingTimes.isEmpty()) {
            double sum = waitingTimes.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / waitingTimes.size();
            double min = waitingTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = waitingTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            System.out.println("\nWaiting Time Summary:");
            System.out.println("Count: " + waitingTimes.size());
            System.out.println("Average: " + avg + " seconds");
            System.out.println("Min: " + min + " seconds");
            System.out.println("Max: " + max + " seconds");
        }

        // Error analysis if a "result" column is present
        if (records.get(0).isMapped("result")) {
            Map<String, Integer> errorCounts = new HashMap<>();
            for (CSVRecord record : records) {
                String result = record.get("result");
                errorCounts.put(result, errorCounts.getOrDefault(result, 0) + 1);
            }
            System.out.println("\nError (result) counts:");
            for (Map.Entry<String, Integer> entry : errorCounts.entrySet()) {
                System.out.println("Result " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    // Analyze job logs: compute job execution time and waiting time summaries
    public static void analyzeJobs(List<CSVRecord> records) {
        System.out.println("\n--- Job Log Analysis ---");
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        List<Double> jobExecutionTimes = new ArrayList<>();

        for (CSVRecord record : records) {
            try {
                if (record.isMapped("started") && record.isMapped("finished")) {
                    String startedStr = record.get("started");
                    String finishedStr = record.get("finished");
                    if (startedStr != null && finishedStr != null && !startedStr.isEmpty() && !finishedStr.isEmpty()) {
                        LocalDateTime start = LocalDateTime.parse(startedStr, dtf);
                        LocalDateTime finish = LocalDateTime.parse(finishedStr, dtf);
                        Duration duration = Duration.between(start, finish);
                        double seconds = duration.toMillis() / 1000.0;
                        jobExecutionTimes.add(seconds);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing job dates: " + e.getMessage());
            }
        }

        if (!jobExecutionTimes.isEmpty()) {
            double sum = jobExecutionTimes.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / jobExecutionTimes.size();
            double min = jobExecutionTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = jobExecutionTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            System.out.println("Job Execution Time Summary:");
            System.out.println("Count: " + jobExecutionTimes.size());
            System.out.println("Average: " + avg + " seconds");
            System.out.println("Min: " + min + " seconds");
            System.out.println("Max: " + max + " seconds");
        }

        // Analyze job waiting times (if available)
        List<Double> waitingTimes = new ArrayList<>();
        for (CSVRecord record : records) {
            try {
                if (record.isMapped("waitingtime")) {
                    String waitingStr = record.get("waitingtime");
                    if (waitingStr != null && !waitingStr.isEmpty()) {
                        double wt = Double.parseDouble(waitingStr);
                        waitingTimes.add(wt);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing job waiting time: " + e.getMessage());
            }
        }
        if (!waitingTimes.isEmpty()) {
            double sum = waitingTimes.stream().mapToDouble(Double::doubleValue).sum();
            double avg = sum / waitingTimes.size();
            double min = waitingTimes.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = waitingTimes.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            System.out.println("\nJob Waiting Time Summary:");
            System.out.println("Count: " + waitingTimes.size());
            System.out.println("Average: " + avg + " seconds");
            System.out.println("Min: " + min + " seconds");
            System.out.println("Max: " + max + " seconds");
        }
    }

    // Analyze component logs: count messages by log level and error codes
    public static void analyzeComponentLogs(List<CSVRecord> records) {
        System.out.println("\n--- Component Log Analysis ---");
        Map<String, Integer> logLevelCounts = new HashMap<>();
        for (CSVRecord record : records) {
            if (record.isMapped("loglevel")) {
                String level = record.get("loglevel");
                logLevelCounts.put(level, logLevelCounts.getOrDefault(level, 0) + 1);
            }
        }
        System.out.println("Log Level Counts:");
        for (Map.Entry<String, Integer> entry : logLevelCounts.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // If an "error" column exists, count error occurrences
        if (records.get(0).isMapped("error")) {
            Map<String, Integer> errorCounts = new HashMap<>();
            for (CSVRecord record : records) {
                String errorVal = record.get("error");
                errorCounts.put(errorVal, errorCounts.getOrDefault(errorVal, 0) + 1);
            }
            System.out.println("\nComponent Error Counts:");
            for (Map.Entry<String, Integer> entry : errorCounts.entrySet()) {
                System.out.println("Error " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
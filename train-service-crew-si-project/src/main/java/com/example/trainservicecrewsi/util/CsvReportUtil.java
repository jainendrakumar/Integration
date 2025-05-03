package com.example.trainservicecrewsi.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Utility component for writing transaction reports to monthly CSV files.
 */
@Component
public class CsvReportUtil {

    private static final Logger log = LoggerFactory.getLogger(CsvReportUtil.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME; // Use ISO format for clarity
    private static final String[] HEADERS = {"MessageArrivalTime", "RequestID", "Status", "MessageSentTime", "Context"};

    private final Path reportDir;
    private final String reportFilePrefix;
    private final Lock lock = new ReentrantLock(); // Lock for thread-safe file access

    /**
     * Constructor initializes report directory and file prefix from application properties.
     *
     * @param reportFolder Path to the report directory.
     * @param reportPrefix Prefix for report filenames.
     */
    public CsvReportUtil(@Value("${app.report.csv.folder:reports}") String reportFolder,
                         @Value("${app.report.csv.prefix:train_service_report_}") String reportPrefix) {
        this.reportDir = Paths.get(reportFolder);
        this.reportFilePrefix = reportPrefix;
        createReportDirectory();
    }

    private void createReportDirectory() {
        try {
            Files.createDirectories(reportDir);
        } catch (IOException e) {
            log.error("Failed to create report directory: {}", reportDir, e);
            // Application might still function, but reporting will fail.
        }
    }

    /**
     * Writes a report entry to the appropriate monthly CSV file.
     * This method is synchronized to ensure thread safety when accessing the file.
     *
     * @param arrivalTime The time the message arrived.
     * @param requestId   The request ID.
     * @param status      The processing status (e.g., "OK", "Processed", "Error").
     * @param sentTime    The time the response message was sent.
     * @param context     The context of the operation (e.g., "CrewRequestEndpoint", "StatusEndpoint").
     */
    public void writeReportEntry(Instant arrivalTime, String requestId, String status, Instant sentTime, String context) {
        lock.lock(); // Acquire lock before accessing file
        try {
            YearMonth currentMonth = YearMonth.from(arrivalTime.atZone(ZoneId.systemDefault()));
            String filename = String.format("%s%s.csv", reportFilePrefix, currentMonth.format(MONTH_FORMATTER));
            Path filePath = reportDir.resolve(filename);

            boolean fileExists = Files.exists(filePath);

            // Use try-with-resources for automatic resource management
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT))
            {
                // Write header only if the file is newly created in this operation
                if (!fileExists) {
                    csvPrinter.printRecord((Object[]) HEADERS);
                    log.info("Created new report file and wrote headers: {}", filePath);
                }

                // Format timestamps using ISO standard
                String formattedArrivalTime = TIMESTAMP_FORMATTER.format(arrivalTime.atZone(ZoneId.systemDefault()));
                String formattedSentTime = TIMESTAMP_FORMATTER.format(sentTime.atZone(ZoneId.systemDefault()));

                // Write the actual data record
                csvPrinter.printRecord(formattedArrivalTime, requestId, status, formattedSentTime, context);
                log.debug("Appended report entry for Request ID: {} to file: {}", requestId, filePath);

            } catch (IOException e) {
                log.error("Failed to write report entry for Request ID: {} to file: {}", requestId, filePath, e);
            }
        } finally {
            lock.unlock(); // Release lock in finally block
        }
    }
}


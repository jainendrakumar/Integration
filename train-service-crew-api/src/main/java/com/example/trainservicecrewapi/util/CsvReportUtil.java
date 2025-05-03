package com.example.trainservicecrewapi.util;

import com.example.trainservicecrewapi.model.StatusResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for generating monthly CSV reports.
 * Logs message arrival time, request ID, status, and message sent time.
 */
@Component
public class CsvReportUtil {

    private static final Logger log = LoggerFactory.getLogger(CsvReportUtil.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // Standard format

    @Value("${app.report.csv.folder:reports}")
    private String reportFolder;

    @Value("${app.report.csv.prefix:train_service_report_}")
    private String reportPrefix;

    private static final String[] HEADERS = {"MessageArrivalTime", "RequestID", "Status", "MessageSentTime", "Context"};

    /**
     * Appends a record to the appropriate monthly CSV report file.
     * Creates the file and writes headers if it doesn't exist.
     *
     * @param arrivalTime The time the message arrived.
     * @param requestId The request ID.
     * @param status The processing status (e.g., "OK", "Processed", "Error").
     * @param sentTime The time the response message was sent.
     * @param context A string indicating the context (e.g., "Endpoint1", "Endpoint2").
     */
    public synchronized void writeReportEntry(LocalDateTime arrivalTime, String requestId, String status, LocalDateTime sentTime, String context) {
        YearMonth currentMonth = YearMonth.from(arrivalTime);
        String filename = reportPrefix + currentMonth.format(MONTH_FORMATTER) + ".csv";
        Path reportDirPath = Paths.get(reportFolder);
        Path reportFilePath = reportDirPath.resolve(filename);

        try {
            Files.createDirectories(reportDirPath); // Ensure directory exists

            boolean fileExists = Files.exists(reportFilePath);

            // Use try-with-resources for BufferedWriter and CSVPrinter
            try (BufferedWriter writer = Files.newBufferedWriter(reportFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                 CSVPrinter csvPrinter = new CSVPrinter(writer, fileExists ? CSVFormat.DEFAULT : CSVFormat.DEFAULT.withHeader(HEADERS))) {

                csvPrinter.printRecord(
                        arrivalTime.format(TIMESTAMP_FORMATTER),
                        requestId,
                        status,
                        sentTime.format(TIMESTAMP_FORMATTER),
                        context
                );
                csvPrinter.flush(); // Ensure data is written
                log.debug("Successfully wrote entry to CSV report: {}", reportFilePath);

            } // CSVPrinter and writer are automatically closed here

        } catch (IOException e) {
            log.error("Failed to write entry to CSV report file {}: {}", reportFilePath, e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while writing to CSV report {}: {}", reportFilePath, e.getMessage(), e);
        }
    }

    /**
     * Overloaded method specifically for logging the final status response from the second endpoint.
     *
     * @param arrivalTime The time the status request arrived.
     * @param statusResponse The StatusResponse object containing details.
     * @param sentTime The time the final status response was sent.
     */
    public void logStatusResponseReport(LocalDateTime arrivalTime, StatusResponse statusResponse, LocalDateTime sentTime) {
        writeReportEntry(arrivalTime, statusResponse.getRequestID(), statusResponse.getSTATUS(), sentTime, "StatusEndpoint");
    }
}


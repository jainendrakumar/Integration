package com.example.aggregation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * ReportService aggregates per-minute metrics for incoming and outgoing messages,
 * and flushes the data to CSV files daily. It uses a ConcurrentSkipListMap to maintain
 * a time-ordered map of ReportRecord objects.
 *
 * @author JKR3
 */
@Service
public class ReportService {

    // Formatter for minute-level keys (e.g., "yyyyMMdd_HHmm")
    private final DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    // Formatter for daily CSV file naming (e.g., "yyyyMMdd")
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Use a ConcurrentSkipListMap for time-ordered keys.
    private final ConcurrentSkipListMap<String, ReportRecord> incomingRecords = new ConcurrentSkipListMap<>();
    private final ConcurrentSkipListMap<String, ReportRecord> outgoingRecords = new ConcurrentSkipListMap<>();

    // Report directories for incoming and outgoing CSV reports; configurable via properties.
    @Value("${report.incoming.dir:report/incoming}")
    private String reportIncomingDir;

    @Value("${report.outgoing.dir:report/outgoing}")
    private String reportOutgoingDir;

    // Flush threshold in minutes (e.g., flush any record older than 1 minute).
    @Value("${report.flush.threshold.minutes:1}")
    private long flushThresholdMinutes;

    /**
     * Records an incoming message metric by updating the ReportRecord for the current minute.
     *
     * @param message the incoming message as a String.
     */
    public void recordIncoming(String message) {
        int bytes = message.getBytes(StandardCharsets.UTF_8).length;
        String minuteKey = LocalDateTime.now().format(minuteFormatter);
        incomingRecords.computeIfAbsent(minuteKey, ReportRecord::new)
                .add(1, bytes, 1);
        System.out.println("Recorded incoming metric for key: " + minuteKey);
    }

    /**
     * Records an outgoing message metric by updating the ReportRecord for the current minute.
     *
     * @param message the outgoing message as a String.
     */
    public void recordOutgoing(String message) {
        int bytes = message.getBytes(StandardCharsets.UTF_8).length;
        String minuteKey = LocalDateTime.now().format(minuteFormatter);
        outgoingRecords.computeIfAbsent(minuteKey, ReportRecord::new)
                .add(1, bytes, 0);
        System.out.println("Recorded outgoing metric for key: " + minuteKey);
    }

    /**
     * Scheduled task that flushes all ReportRecords older than the threshold to CSV files.
     * This method runs at the top of every minute.
     */
    @Scheduled(cron = "0 * * * * *")
    public void flushReports() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(flushThresholdMinutes);
        System.out.println("Flushing reports. Current time: " + now + ", threshold: " + threshold);

        // Flush incoming records
        flushRecords(incomingRecords, reportIncomingDir, true);
        // Flush outgoing records
        flushRecords(outgoingRecords, reportOutgoingDir, false);
    }

    /**
     * Iterates over the provided records map and flushes any entries whose key time is before the threshold.
     *
     * @param records   the map of ReportRecord objects.
     * @param dir       the directory to write the CSV file.
     * @param isIncoming true if processing incoming metrics (includes combined count).
     */
    private void flushRecords(ConcurrentSkipListMap<String, ReportRecord> records, String dir, boolean isIncoming) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(flushThresholdMinutes);
        // Iterate over the keys in order.
        Iterator<Map.Entry<String, ReportRecord>> iterator = records.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ReportRecord> entry = iterator.next();
            try {
                LocalDateTime recordTime = LocalDateTime.parse(entry.getKey(), minuteFormatter);
                if (recordTime.isBefore(threshold)) {
                    String dayKey = recordTime.format(dateFormatter);
                    writeCsvLine(dir, (isIncoming ? "incoming_report_" : "outgoing_report_") + dayKey + ".csv", entry.getKey(), entry.getValue(), isIncoming);
                    System.out.println("Flushed " + (isIncoming ? "incoming" : "outgoing") + " record for key: " + entry.getKey());
                    iterator.remove();
                }
            } catch (Exception e) {
                System.err.println("Error processing record with key: " + entry.getKey());
                e.printStackTrace();
            }
        }
    }

    /**
     * Writes a single line to a CSV file. Creates the directory if it doesn't exist and writes a header if the file is new.
     *
     * @param dir         the report directory.
     * @param fileName    the CSV file name.
     * @param minuteKey   the key for the record.
     * @param record      the ReportRecord containing metrics.
     * @param isIncoming  true if writing incoming metrics.
     */
    private void writeCsvLine(String dir, String fileName, String minuteKey, ReportRecord record, boolean isIncoming) {
        try {
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(fileName);
            boolean fileExists = Files.exists(filePath);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (!fileExists) {
                    if (isIncoming) {
                        writer.write("Minute,MessageCount,TotalBytes,CombinedCount");
                    } else {
                        writer.write("Minute,MessageCount,TotalBytes");
                    }
                    writer.newLine();
                }
                if (isIncoming) {
                    writer.write(minuteKey + "," + record.getMessageCount() + "," + record.getTotalBytes() + "," + record.getCombinedCount());
                } else {
                    writer.write(minuteKey + "," + record.getMessageCount() + "," + record.getTotalBytes());
                }
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("Error writing CSV line for key: " + minuteKey);
            e.printStackTrace();
        }
    }
}

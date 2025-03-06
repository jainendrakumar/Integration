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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ReportService collects reporting metrics for incoming and outgoing messages.
 * It maintains per-minute metrics (message count, total bytes, and for incoming, combined message count)
 * and writes them to CSV files. A new CSV file is generated each day.
 *
 * @author JKR3
 */
@Service
public class ReportService {

    // Formatter for minute-level keys (e.g., yyyyMMdd_HHmm)
    private final DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    // Formatter for daily filenames (e.g., yyyyMMdd)
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Concurrent maps to store metrics per minute.
    private final ConcurrentMap<String, Metric> incomingMetrics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Metric> outgoingMetrics = new ConcurrentHashMap<>();

    // Configurable directories for CSV reports.
    @Value("${report.incoming.dir:report/incoming}")
    private String reportIncomingDir;

    @Value("${report.outgoing.dir:report/outgoing}")
    private String reportOutgoingDir;

    /**
     * Records an incoming message.
     * Each call increments:
     * - Count (number of incoming events)
     * - Total bytes (size in bytes)
     * - Combined count (number of messages that have been combined; here each call is one unit)
     *
     * @param message the incoming JSON message.
     */
    public void recordIncoming(String message) {
        int bytes = message.getBytes(StandardCharsets.UTF_8).length;
        String minuteKey = LocalDateTime.now().format(minuteFormatter);
        incomingMetrics.compute(minuteKey, (key, metric) -> {
            if (metric == null) {
                metric = new Metric();
            }
            metric.increment(1, bytes, 1);
            return metric;
        });
    }

    /**
     * Records an outgoing (merged) message.
     * Each call increments:
     * - Count (number of outgoing events)
     * - Total bytes (size in bytes)
     *
     * @param message the outgoing JSON message.
     */
    public void recordOutgoing(String message) {
        int bytes = message.getBytes(StandardCharsets.UTF_8).length;
        String minuteKey = LocalDateTime.now().format(minuteFormatter);
        outgoingMetrics.compute(minuteKey, (key, metric) -> {
            if (metric == null) {
                metric = new Metric();
            }
            metric.increment(1, bytes, 0);
            return metric;
        });
    }

    /**
     * Scheduled task that runs at the top of every minute.
     * It flushes metrics for the previous minute to CSV files.
     */
    @Scheduled(cron = "0 * * * * *")
    public void flushReports() {
        // Determine the previous minute.
        LocalDateTime previousMinuteTime = LocalDateTime.now().minusMinutes(1);
        String minuteKey = previousMinuteTime.format(minuteFormatter);
        String dayKey = previousMinuteTime.format(dateFormatter);

        // Flush incoming metrics.
        Metric inMetric = incomingMetrics.remove(minuteKey);
        if (inMetric != null) {
            writeCsvLine(reportIncomingDir, "incoming_report_" + dayKey + ".csv", minuteKey, inMetric, true);
        }

        // Flush outgoing metrics.
        Metric outMetric = outgoingMetrics.remove(minuteKey);
        if (outMetric != null) {
            writeCsvLine(reportOutgoingDir, "outgoing_report_" + dayKey + ".csv", minuteKey, outMetric, false);
        }
    }

    /**
     * Writes a line to a CSV file.
     *
     * @param dir the report directory.
     * @param fileName the name of the CSV file (includes the date).
     * @param minuteKey the minute key (yyyyMMdd_HHmm).
     * @param metric the metric data.
     * @param isIncoming true if writing the incoming report (includes combined count).
     */
    private void writeCsvLine(String dir, String fileName, String minuteKey, Metric metric, boolean isIncoming) {
        try {
            Path dirPath = Paths.get(dir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            Path filePath = dirPath.resolve(fileName);
            boolean fileExists = Files.exists(filePath);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                if (!fileExists) {
                    // Write CSV header.
                    if (isIncoming) {
                        writer.write("Minute,MessageCount,TotalBytes,CombinedCount");
                    } else {
                        writer.write("Minute,MessageCount,TotalBytes");
                    }
                    writer.newLine();
                }
                if (isIncoming) {
                    writer.write(minuteKey + "," + metric.getCount() + "," + metric.getTotalBytes() + "," + metric.getCombinedCount());
                } else {
                    writer.write(minuteKey + "," + metric.getCount() + "," + metric.getTotalBytes());
                }
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Metric class holds the per-minute counts and total bytes.
     */
    public static class Metric {
        private long count;
        private long totalBytes;
        // For incoming messages, tracks the number of messages combined.
        private long combinedCount;

        /**
         * Increments the metrics.
         *
         * @param cnt the count increment.
         * @param bytes the bytes increment.
         * @param combined the combined count increment.
         */
        public synchronized void increment(long cnt, long bytes, long combined) {
            this.count += cnt;
            this.totalBytes += bytes;
            this.combinedCount += combined;
        }

        public long getCount() {
            return count;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public long getCombinedCount() {
            return combinedCount;
        }
    }
}

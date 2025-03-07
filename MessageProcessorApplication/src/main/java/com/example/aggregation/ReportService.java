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
 * ReportService collects per-minute metrics for incoming and outgoing messages
 * and writes them to daily CSV files.
 *
 * @author JKR3
 */
@Service
public class ReportService {

    private final DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ConcurrentMap<String, Metric> incomingMetrics = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Metric> outgoingMetrics = new ConcurrentHashMap<>();

    @Value("${report.incoming.dir:report/incoming}")
    private String reportIncomingDir;

    @Value("${report.outgoing.dir:report/outgoing}")
    private String reportOutgoingDir;

    /**
     * Records an incoming message metric.
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
        System.out.println("Recorded incoming message for minute: " + minuteKey);
    }

    /**
     * Records an outgoing message metric.
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
        System.out.println("Recorded outgoing message for minute: " + minuteKey);
    }

    /**
     * Scheduled task that flushes metrics for the previous minute to CSV files.
     */
    @Scheduled(cron = "0 * * * * *")
    public void flushReports() {
        System.out.println("Flushing reports for the previous minute...");
        LocalDateTime previousMinuteTime = LocalDateTime.now().minusMinutes(2);
        String minuteKey = previousMinuteTime.format(minuteFormatter);
        String dayKey = previousMinuteTime.format(dateFormatter);
        System.out.println("Flushing reports for minute: " + minuteKey);

        Metric inMetric = incomingMetrics.remove(minuteKey);
        if (inMetric != null) {
            writeCsvLine(reportIncomingDir, "incoming_report_" + dayKey + ".csv", minuteKey, inMetric, true);
            System.out.println("Wrote incoming report for " + minuteKey);
        } else {
            System.out.println("No incoming metrics for " + minuteKey);
        }

        Metric outMetric = outgoingMetrics.remove(minuteKey);
        if (outMetric != null) {
            writeCsvLine(reportOutgoingDir, "outgoing_report_" + dayKey + ".csv", minuteKey, outMetric, false);
            System.out.println("Wrote outgoing report for " + minuteKey);
        } else {
            System.out.println("No outgoing metrics for " + minuteKey);
        }
    }

    /**
     * Writes a CSV line to the specified file.
     */
    private void writeCsvLine(String dir, String fileName, String minuteKey, Metric metric, boolean isIncoming) {
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
     * Simple Metric class.
     */
    public static class Metric {
        private long count;
        private long totalBytes;
        private long combinedCount;

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
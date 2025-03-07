package com.example.aggregation;

/**
 * ReportRecord holds aggregated metric data for a specific minute.
 *
 * @author JKR3
 */
public class ReportRecord {
    private final String minuteKey;
    private long messageCount;
    private long totalBytes;
    // For incoming metrics, we track an additional combined count.
    private long combinedCount;

    public ReportRecord(String minuteKey) {
        this.minuteKey = minuteKey;
        this.messageCount = 0;
        this.totalBytes = 0;
        this.combinedCount = 0;
    }

    public synchronized void add(long count, long bytes, long combined) {
        this.messageCount += count;
        this.totalBytes += bytes;
        this.combinedCount += combined;
    }

    public String getMinuteKey() {
        return minuteKey;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getCombinedCount() {
        return combinedCount;
    }
}

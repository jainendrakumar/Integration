package com.qserver;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Analyzes data to identify trends, anomalies, and performance bottlenecks.
 */
public class DataAnalyzer {

    /**
     * Analyzes data and calculates statistics.
     *
     * @param records The list of CSV records to analyze.
     */
    public static void analyzeData(List<CSVRecord> records) {
        // Group by transaction ID
        Map<String, List<CSVRecord>> transactionsById = records.stream()
                .collect(Collectors.groupingBy(record -> record.get("transactionid")));

        // Calculate statistics for each transaction
        transactionsById.forEach((id, transactionRecords) -> {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            transactionRecords.forEach(record -> stats.addValue(Double.parseDouble(record.get("length"))));
            System.out.println("Transaction ID: " + id + ", Avg Length: " + stats.getMean());
        });

        // Identify heavily loaded threads
        Map<String, List<CSVRecord>> threadsById = records.stream()
                .collect(Collectors.groupingBy(record -> record.get("threadid")));

        threadsById.forEach((id, threadRecords) -> {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            threadRecords.forEach(record -> stats.addValue(Double.parseDouble(record.get("proctime"))));
            System.out.println("Thread ID: " + id + ", Avg Proc Time: " + stats.getMean());
        });
    }
}
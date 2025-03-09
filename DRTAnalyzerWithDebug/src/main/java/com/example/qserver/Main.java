package com.example.qserver;

import com.example.qserver.analysis.QServerAnalyzer;
import com.example.qserver.model.QServerTransRecord;
import com.example.qserver.parser.CsvParser;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Main is the entry point of the QServer Log Analyzer application.
 * It loads CSV files from a specified directory, performs analysis,
 * and displays the results including charts and summary statistics.
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Load configuration properties
            Properties props = new Properties();
            try (InputStream in = Main.class.getResourceAsStream("/config.properties")) {
                props.load(in);
            }
            String logsDir = props.getProperty("log.directory", "logs");

            // Parse CSV files from the directory
            List<QServerTransRecord> records = CsvParser.parseDirectory(logsDir);
            System.out.println("Loaded " + records.size() + " QServer transaction records.");

            // Create an analyzer instance
            QServerAnalyzer analyzer = new QServerAnalyzer();

            // Example Analysis:
            // 1. Find heavily loaded threads based on processing time.
            Map<String, Double> threadLoad = analyzer.findHeavilyLoadedThreads(records);
            System.out.println("Thread Utilization (Total Processing Time): " + threadLoad);

            // 2. Compute Pearson correlation between waiting time and processing time.
            double correlation = analyzer.computeCorrelation(records);
            System.out.println("Correlation (Waiting Time vs. Processing Time): " + correlation);

            // 3. Identify outlier transactions with high processing time (Z-score > 3.0).
            List<QServerTransRecord> outliers = analyzer.findZScoreOutliers(records, 3.0);
            System.out.println("Found " + outliers.size() + " outlier transactions (Z-score > 3.0).");

            // 4. Display a chart for thread utilization.
            analyzer.chartThreadUtilization(threadLoad);

            // Further filtering, grouping, or reporting can be implemented here.

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

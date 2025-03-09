package com.qserver;

import org.apache.commons.csv.CSVRecord; // Add this import
import java.io.IOException;
import java.util.List;

/**
 * Main class to execute the QServer log analysis program.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Load CSV files
            List<CSVRecord> records = DataLoader.loadCSVFiles("src/main/resources/logs");

            // Process data
            DataProcessor.processData(records);

            // Analyze data
            DataAnalyzer.analyzeData(records);

            // Generate visualizations
            VisualizationEngine.generateCharts(records);

            // Generate report
            ReportGenerator.generateReport(records, "src/main/resources/output/report.xlsx");

            System.out.println("Analysis completed successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package com.qserver;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;
import org.apache.commons.csv.CSVRecord;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates visualizations for analyzed data.
 *
 * @author JKR3
 */
public class VisualizationEngine {

    /**
     * Generates a bar chart for transaction length distribution.
     *
     * @param records The list of CSV records to visualize.
     */
    public static void generateCharts(List<CSVRecord> records) {
        // Create chart
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(600)
                .title("Transaction Length Distribution")
                .xAxisTitle("Transaction ID")
                .yAxisTitle("Length")
                .build();

        // Customize chart
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);

        // Add data
        List<String> transactionIds = records.stream()
                .map(record -> record.get("transactionid"))
                .collect(Collectors.toList());
        List<Double> lengths = records.stream()
                .map(record -> Double.parseDouble(record.get("length")))
                .collect(Collectors.toList());
        chart.addSeries("Transaction Length", transactionIds, lengths);

        // Display chart
        new SwingWrapper<>(chart).displayChart();
    }
}
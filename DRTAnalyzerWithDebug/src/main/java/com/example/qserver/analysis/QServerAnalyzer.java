package com.example.qserver.analysis;

import com.example.qserver.model.QServerTransRecord;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.*;
import java.util.stream.Collectors;

/**
 * QServerAnalyzer provides methods to perform analysis on QServerTransRecord data.
 * It supports filtering, grouping, performance metrics calculation, anomaly detection,
 * correlation analysis, and visualization.
 *
 * @author JKR3
 */
public class QServerAnalyzer {

    /**
     * Filters the list of records by a specific thread name.
     *
     * @param records           List of QServerTransRecord.
     * @param threadNameFilter  The thread name to filter by.
     * @return Filtered list.
     */
    public List<QServerTransRecord> filterRecordsByThreadName(List<QServerTransRecord> records, String threadNameFilter) {
        return records.stream()
                .filter(r -> r.getThreadName().equalsIgnoreCase(threadNameFilter))
                .collect(Collectors.toList());
    }

    /**
     * Groups the records by transaction ID.
     *
     * @param records List of QServerTransRecord.
     * @return Map from transaction ID to list of records.
     */
    public Map<String, List<QServerTransRecord>> groupByTransactionId(List<QServerTransRecord> records) {
        return records.stream().collect(Collectors.groupingBy(QServerTransRecord::getTransactionId));
    }

    /**
     * Finds the top N transactions by processing time.
     *
     * @param records List of QServerTransRecord.
     * @param topN    Number of top records to return.
     * @return List of top transactions.
     */
    public List<QServerTransRecord> findTopTransactionsByProcTime(List<QServerTransRecord> records, int topN) {
        return records.stream()
                .sorted((a, b) -> Double.compare(b.getProcTime(), a.getProcTime()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the total processing time per thread.
     *
     * @param records List of QServerTransRecord.
     * @return Map of thread name to total processing time.
     */
    public Map<String, Double> findHeavilyLoadedThreads(List<QServerTransRecord> records) {
        Map<String, Double> loadMap = new HashMap<>();
        for (QServerTransRecord r : records) {
            loadMap.put(r.getThreadName(),
                    loadMap.getOrDefault(r.getThreadName(), 0.0) + r.getProcTime());
        }
        return loadMap;
    }

    /**
     * Identifies transactions with extreme deviations in processing time using Z-score.
     *
     * @param records   List of QServerTransRecord.
     * @param threshold Z-score threshold (e.g., 3.0).
     * @return List of outlier transactions.
     */
    public List<QServerTransRecord> findZScoreOutliers(List<QServerTransRecord> records, double threshold) {
        List<Double> procTimes = records.stream()
                .map(QServerTransRecord::getProcTime)
                .collect(Collectors.toList());
        double mean = computeMean(procTimes);
        double std = computeStdDev(procTimes, mean);
        List<QServerTransRecord> outliers = new ArrayList<>();
        for (QServerTransRecord r : records) {
            double z = (r.getProcTime() - mean) / (std == 0 ? 1 : std);
            if (z > threshold) {
                outliers.add(r);
            }
        }
        return outliers;
    }

    /**
     * Computes the Pearson correlation coefficient between waiting time and processing time.
     *
     * @param records List of QServerTransRecord.
     * @return Pearson correlation coefficient.
     */
    public double computeCorrelation(List<QServerTransRecord> records) {
        List<Double> xList = records.stream().map(QServerTransRecord::getWaitingTime).collect(Collectors.toList());
        List<Double> yList = records.stream().map(QServerTransRecord::getProcTime).collect(Collectors.toList());
        double meanX = computeMean(xList);
        double meanY = computeMean(yList);
        double sumNum = 0.0;
        double sumDenX = 0.0;
        double sumDenY = 0.0;
        for (int i = 0; i < xList.size(); i++) {
            double dx = xList.get(i) - meanX;
            double dy = yList.get(i) - meanY;
            sumNum += dx * dy;
            sumDenX += dx * dx;
            sumDenY += dy * dy;
        }
        double denom = Math.sqrt(sumDenX * sumDenY);
        return denom == 0 ? 0.0 : sumNum / denom;
    }

    /**
     * Displays a bar chart showing thread utilization based on total processing time.
     *
     * @param threadLoadMap Map of thread name to total processing time.
     */
    public void chartThreadUtilization(Map<String, Double> threadLoadMap) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : threadLoadMap.entrySet()) {
            dataset.addValue(entry.getValue(), "ProcTime", entry.getKey());
        }
        JFreeChart barChart = ChartFactory.createBarChart(
                "Thread Utilization (Processing Time)",
                "Thread Name",
                "Total Processing Time (seconds)",
                dataset);
        ChartFrame frame = new ChartFrame("Thread Utilization", barChart);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    // Helper methods to compute mean and standard deviation.
    private double computeMean(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private double computeStdDev(List<Double> values, double mean) {
        if (values.size() < 2) return 0.0;
        double sumSq = 0.0;
        for (double v : values) {
            sumSq += Math.pow(v - mean, 2);
        }
        return Math.sqrt(sumSq / (values.size() - 1));
    }
}

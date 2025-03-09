package com.qserver;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.csv.CSVRecord;

/**
 * Processes and converts data from CSV records.
 *
 * @author JKR3
 */
public class DataProcessor {

    /**
     * Processes and converts data from CSV records.
     *
     * @param records The list of CSV records to process.
     */
    public static void processData(List<CSVRecord> records) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ssXXX");

        for (CSVRecord record : records) {
            // Convert timestamps
            LocalTime startTime = LocalTime.parse(record.get("starttime"), timeFormatter);
            LocalTime endTime = LocalTime.parse(record.get("endtime"), timeFormatter);

            // Convert numerical values
            double length = Double.parseDouble(record.get("length"));
            double waitingTime = Double.parseDouble(record.get("waitingtime"));
            double procTime = Double.parseDouble(record.get("proctime"));
            double funcTime = Double.parseDouble(record.get("functime"));

            // Add more conversions as needed
        }
    }
}
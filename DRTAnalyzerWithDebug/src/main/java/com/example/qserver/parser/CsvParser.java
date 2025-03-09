package com.example.qserver.parser;

import com.example.qserver.model.QServerTransRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * CsvParser is responsible for reading multiple CSV log files from a directory,
 * parsing each file using Apache Commons CSV, and converting the raw data into
 * a list of QServerTransRecord objects.
 */
public class CsvParser {

    /**
     * Recursively reads all CSV files from the given directory and parses them.
     *
     * @param directoryPath The directory where CSV files are located.
     * @return List of QServerTransRecord objects parsed from the CSV files.
     * @throws IOException If an I/O error occurs.
     */
    public static List<QServerTransRecord> parseDirectory(String directoryPath) throws IOException {
        List<QServerTransRecord> allRecords = new ArrayList<>();
        List<Path> csvFiles = new ArrayList<>();

        // Recursively walk the directory and filter CSV files
        Files.walk(Paths.get(directoryPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
                .forEach(csvFiles::add);

        // Parse each CSV file and aggregate the records
        for (Path path : csvFiles) {
            List<QServerTransRecord> fileRecords = parseCsvFile(path.toString());
            allRecords.addAll(fileRecords);
        }
        return allRecords;
    }

    /**
     * Parses a single CSV file and converts its rows into QServerTransRecord objects.
     *
     * @param csvFilePath The path to the CSV file.
     * @return List of QServerTransRecord objects.
     * @throws IOException If an I/O error occurs.
     */
    public static List<QServerTransRecord> parseCsvFile(String csvFilePath) throws IOException {
        List<QServerTransRecord> records = new ArrayList<>();

        try (Reader reader = new FileReader(csvFilePath);
             CSVParser parser = CSVFormat.DEFAULT
                     .withDelimiter('\t')           // Use tab as delimiter; change to ',' if needed
                     .withFirstRecordAsHeader()
                     .parse(reader)) {

            for (CSVRecord row : parser) {
                QServerTransRecord rec = new QServerTransRecord();

                // Parse string fields
                rec.setTransactionId(getString(row, "transactionid"));
                rec.setTransactionKind(getString(row, "transactionkind"));
                rec.setThreadName(getString(row, "threadname"));
                rec.setThreadId(getString(row, "threadid"));
                rec.setClientId(getString(row, "clientid"));
                rec.setIpClient(getString(row, "ipclient"));
                rec.setUsername(getString(row, "username"));
                rec.setActionElementType(getString(row, "actionelementtype"));
                rec.setActionElementName(getString(row, "actionelementname"));
                rec.setActionElementKey(getString(row, "actionelementkey"));
                rec.setDescription(getString(row, "description"));
                rec.setMessageId(getString(row, "messageid"));
                rec.setLockProfile(getString(row, "lockprofile"));

                // Parse timestamps; assumes log times are provided as "HH:mm:ss+offset"
                LocalDateTime st = parseTimeWithOffset(getString(row, "starttime"));
                LocalDateTime et = parseTimeWithOffset(getString(row, "endtime"));
                rec.setStartTime(st);
                rec.setEndTime(et);

                // Parse numeric fields
                rec.setLength(parseDouble(row, "length"));
                rec.setWaitingTime(parseDouble(row, "waitingtime"));
                rec.setProcTime(parseDouble(row, "proctime"));
                rec.setFuncTime(parseDouble(row, "functime"));
                rec.setDeltaSetFinalize(parseDouble(row, "deltasetfinalize"));
                rec.setIoDef(parseDouble(row, "iodef"));
                rec.setNotifications(parseDouble(row, "notifications"));
                rec.setMrSend(parseDouble(row, "mrsend"));
                rec.setDbTime(parseDouble(row, "dbtime"));
                rec.setStreamTime(parseDouble(row, "streamtime"));
                rec.setNrDatasets(parseDouble(row, "nrdatasets"));
                rec.setSize(parseDouble(row, "size"));
                rec.setConstructions(parseDouble(row, "constructions"));
                rec.setDestructions(parseDouble(row, "destructions"));
                rec.setChanges(parseDouble(row, "changes"));
                rec.setProcMem(parseDouble(row, "procmem"));
                rec.setFuncMem(parseDouble(row, "funcmem"));
                rec.setDbMem(parseDouble(row, "dbmem"));

                // If length is zero, optionally derive it from start and end times
                if (rec.getLength() == 0.0 && st != null && et != null) {
                    double computedLength = (double) (java.time.Duration.between(st, et).toMillis()) / 1000.0;
                    if (computedLength > 0) {
                        rec.setLength(computedLength);
                    }
                }
                records.add(rec);
            }
        }
        return records;
    }

    /**
     * Helper method to safely retrieve a string value from a CSVRecord.
     *
     * @param row CSVRecord row.
     * @param col Column name.
     * @return The string value, or an empty string if not present.
     */
    private static String getString(CSVRecord row, String col) {
        return row.isMapped(col) ? row.get(col) : "";
    }

    /**
     * Helper method to parse a numeric field from a CSVRecord.
     *
     * @param row CSVRecord row.
     * @param col Column name.
     * @return Parsed double value, or 0.0 if parsing fails.
     */
    private static double parseDouble(CSVRecord row, String col) {
        String val = getString(row, col);
        if (val.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Parses a time string in the format "HH:mm:ss+offset" (e.g., "16:10:42+05:30")
     * and combines it with the current date to produce a LocalDateTime.
     *
     * @param timeStr Time string to parse.
     * @return LocalDateTime instance, or null if parsing fails.
     */
    private static LocalDateTime parseTimeWithOffset(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        try {
            // Parse as OffsetTime and combine with today's date.
            OffsetTime offsetTime = OffsetTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ssXXX"));
            return LocalDate.now().atTime(offsetTime.toLocalTime());
        } catch (Exception e) {
            return null;
        }
    }
}

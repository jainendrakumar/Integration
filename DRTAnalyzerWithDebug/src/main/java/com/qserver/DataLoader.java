package com.qserver;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and parses CSV files from a specified directory.
 */
public class DataLoader {

    /**
     * Loads and parses CSV files from the specified directory.
     *
     * @param directory The directory containing CSV files.
     * @return A list of CSVRecord objects.
     * @throws IOException If an I/O error occurs.
     */
    public static List<CSVRecord> loadCSVFiles(String directory) throws IOException {
        List<CSVRecord> records = new ArrayList<>();
        Files.list(Paths.get(directory))
                .filter(path -> path.toString().endsWith(".csv"))
                .forEach(path -> {
                    try (FileReader reader = new FileReader(path.toFile());
                         CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {
                        records.addAll(csvParser.getRecords());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        return records;
    }
}
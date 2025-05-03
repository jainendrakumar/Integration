package com.example.trainservicecrewapi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for logging raw request, response, and error JSON strings to files.
 * Reads target directories from application properties.
 */
@Component
public class FileLoggerUtil {

    private static final Logger log = LoggerFactory.getLogger(FileLoggerUtil.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    @Value("${app.log.received.folder:logs/received}")
    private String receivedLogFolder;

    @Value("${app.log.sent.folder:logs/sent}")
    private String sentLogFolder;

    @Value("${app.log.error.folder:logs/error}")
    private String errorLogFolder;

    /**
     * Logs the given JSON content to a specified log type folder.
     * Creates a timestamped file for each log entry.
     *
     * @param logType The type of log ("received", "sent", "error").
     * @param requestId The request ID associated with the log entry, used in the filename.
     * @param jsonContent The JSON string content to log.
     */
    public void logJsonToFile(String logType, String requestId, String jsonContent) {
        String folderPath;
        switch (logType.toLowerCase()) {
            case "received":
                folderPath = receivedLogFolder;
                break;
            case "sent":
                folderPath = sentLogFolder;
                break;
            case "error":
                folderPath = errorLogFolder;
                break;
            default:
                log.error("Invalid log type specified: {}", logType);
                return;
        }

        try {
            Path directory = Paths.get(folderPath);
            Files.createDirectories(directory); // Ensure directory exists

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String safeRequestId = (requestId == null || requestId.isEmpty()) ? "unknown" : requestId.replaceAll("[^a-zA-Z0-9.-]", "_");
            String filename = String.format("%s_%s_%s.json", logType, safeRequestId, timestamp);
            Path filePath = directory.resolve(filename);

            Files.writeString(filePath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            log.debug("Successfully logged {} JSON to file: {}", logType, filePath);

        } catch (IOException e) {
            log.error("Failed to log {} JSON to file for request ID {}: {}", logType, requestId, e.getMessage(), e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while logging {} JSON for request ID {}: {}", logType, requestId, e.getMessage(), e);
        }
    }
}


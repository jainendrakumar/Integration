package com.example.trainservicecrewsi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility component for logging raw JSON messages to files.
 */
@Component
public class FileLoggerUtil {

    private static final Logger log = LoggerFactory.getLogger(FileLoggerUtil.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path receivedLogDir;
    private final Path sentLogDir;
    private final Path errorLogDir;

    /**
     * Constructor initializes log directories based on application properties.
     *
     * @param receivedFolder Path to the received logs folder.
     * @param sentFolder     Path to the sent logs folder.
     * @param errorFolder    Path to the error logs folder.
     */
    public FileLoggerUtil(@Value("${app.log.received.folder:logs/received}") String receivedFolder,
                          @Value("${app.log.sent.folder:logs/sent}") String sentFolder,
                          @Value("${app.log.error.folder:logs/error}") String errorFolder) {
        this.receivedLogDir = Paths.get(receivedFolder);
        this.sentLogDir = Paths.get(sentFolder);
        this.errorLogDir = Paths.get(errorFolder);
        createDirectories();
    }

    private void createDirectories() {
        try {
            Files.createDirectories(receivedLogDir);
            Files.createDirectories(sentLogDir);
            Files.createDirectories(errorLogDir);
        } catch (IOException e) {
            log.error("Failed to create log directories", e);
            // Application might still function, but logging will fail.
        }
    }

    /**
     * Logs a JSON string to the appropriate directory based on the log type.
     *
     * @param logType   The type of log ("receivedRequest", "sentResponse", "receivedStatus", "sentStatusResponse", "error").
     * @param requestId The request ID (can be null).
     * @param jsonContent The JSON content to log.
     */
    public void logJsonToFile(String logType, String requestId, String jsonContent) {
        if (jsonContent == null) {
            log.warn("Attempted to log null JSON content for type: {}, ID: {}", logType, requestId);
            return;
        }

        Path targetDir;
        String filePrefix;

        switch (logType) {
            case "receivedRequest":
            case "receivedStatus":
                targetDir = receivedLogDir;
                filePrefix = "received";
                break;
            case "sentResponse":
            case "sentStatusResponse":
                targetDir = sentLogDir;
                filePrefix = "sent";
                break;
            case "error":
                targetDir = errorLogDir;
                filePrefix = "error";
                break;
            default:
                log.warn("Unknown log type received: {}", logType);
                targetDir = errorLogDir; // Log unknown types as errors
                filePrefix = "unknown_type";
                break;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String safeRequestId = (requestId == null || requestId.trim().isEmpty()) ? "unknown" : requestId.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String filename = String.format("%s_%s_%s.json", filePrefix, safeRequestId, timestamp);
        Path filePath = targetDir.resolve(filename);

        try {
            Files.writeString(filePath, jsonContent, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            log.debug("Successfully logged {} to file: {}", logType, filePath);
        } catch (IOException e) {
            log.error("Failed to write {} log to file: {}", logType, filePath, e);
        }
    }
}


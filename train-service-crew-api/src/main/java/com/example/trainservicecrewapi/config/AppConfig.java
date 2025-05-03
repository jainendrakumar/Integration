package com.example.trainservicecrewapi.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.trainservicecrewapi.service.TrainCrewService; // Assuming service has error logging
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;

/**
 * Configuration class for the Spring Boot application.
 * Includes beans for ObjectMapper customization and global exception handling.
 */
@Configuration
public class AppConfig {

    /**
     * Configures and provides a customized ObjectMapper bean.
     * - Registers the JavaTimeModule for proper serialization/deserialization of Java 8 date/time types.
     * - Configures the mapper to ignore unknown properties during deserialization to prevent errors
     *   if the incoming JSON has extra fields not defined in the model classes.
     *
     * @return A configured ObjectMapper instance.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Ignore properties in JSON that are not mapped to fields in the Java object
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}

/**
 * Global Exception Handler for the REST controllers.
 * Catches specific exceptions and returns appropriate HTTP responses.
 */
@ControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Inject TrainCrewService or FileLoggerUtil if direct error logging to file is needed here
    // @Autowired
    // private TrainCrewService trainCrewService; // Example
    // @Autowired
    // private FileLoggerUtil fileLoggerUtil; // Example

    /**
     * Handles JSON processing exceptions (e.g., malformed JSON) during request body deserialization.
     * Logs the error and returns an HTTP 400 Bad Request response, as specified in requirement 7.
     *
     * @param ex The caught JsonProcessingException.
     * @return A ResponseEntity with HTTP status 400.
     */
    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Object> handleJsonProcessingException(JsonProcessingException ex) {
        LocalDateTime errorTime = LocalDateTime.now();
        String errorMsg = "Malformed JSON request: " + ex.getOriginalMessage();
        log.error(errorMsg, ex);

        // Optionally log this error to the error file using FileLoggerUtil or via the service
        // fileLoggerUtil.logJsonToFile("error", "unknown_request", "{\"error\":\"" + errorMsg + "\"}");

        // Per requirement 7, return 400 Malformed request status without sending error details in response body
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    /**
     * Handles generic exceptions that are not caught by more specific handlers.
     * Logs the error and returns an HTTP 500 Internal Server Error response.
     *
     * @param ex The caught Exception.
     * @return A ResponseEntity with HTTP status 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        LocalDateTime errorTime = LocalDateTime.now();
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        // Optionally log this error to the error file
        // fileLoggerUtil.logJsonToFile("error", "unknown_request", "{\"error\":\"Internal Server Error\", \"detail\":\"" + ex.getMessage() + "\"}");

        // Return 500 Internal Server Error for unexpected issues
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}


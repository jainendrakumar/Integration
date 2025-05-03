package com.example.trainservicecrewsi.config;

import com.example.trainservicecrewsi.util.CsvReportUtil;
import com.example.trainservicecrewsi.util.FileLoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel; // Added import
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import static com.example.trainservicecrewsi.config.IntegrationConfig.*;

/**
 * Spring Integration configuration specifically for logging flows.
 */
@Configuration
public class LoggingIntegrationConfig {

    private static final Logger log = LoggerFactory.getLogger(LoggingIntegrationConfig.class);

    /**
     * Integration flow that subscribes to the loggingChannel and routes messages
     * to the file logging service activator.
     *
     * @param fileLogHandler The handler bean for logging to files.
     * @param loggingChannel The channel to subscribe to (injected by Spring).
     * @return IntegrationFlow definition.
     */
    @Bean
    public IntegrationFlow fileLoggingFlow(FileLogHandler fileLogHandler, 
                                           @Qualifier("loggingChannel") MessageChannel loggingChannel) { // Inject channel
        return IntegrationFlow.from(loggingChannel) // Subscribe to the injected logging channel
                .handle(fileLogHandler, "handleLogMessage")
                .get();
    }

    /**
     * Component responsible for handling messages sent to the loggingChannel
     * and delegating to the FileLoggerUtil.
     */
    @Component
    static class FileLogHandler {
        private final FileLoggerUtil fileLoggerUtil;

        FileLogHandler(FileLoggerUtil fileLoggerUtil) {
            this.fileLoggerUtil = fileLoggerUtil;
        }

        public void handleLogMessage(Message<?> message) {
            Object payload = message.getPayload();
            MessageHeaders headers = message.getHeaders();
            log.debug("Received message on loggingChannel for file logging.");
            String logType = headers.get("logType", String.class);
            // Use full class name for constants to be safe
            String requestId = headers.get(IntegrationConfig.HEADER_REQUEST_ID, String.class);
            String jsonContent = null;

            if (logType != null) {
                switch (logType) {
                    case "receivedRequest":
                    case "receivedStatus":
                        jsonContent = headers.get(IntegrationConfig.HEADER_REQUEST_JSON, String.class);
                        break;
                    case "sentResponse":
                        jsonContent = headers.get(IntegrationConfig.HEADER_RESPONSE_JSON, String.class);
                        break;
                    case "sentStatusResponse":
                        jsonContent = headers.get(IntegrationConfig.HEADER_STATUS_RESPONSE_JSON, String.class);
                        break;
                    case "error":
                        String exceptionMessage = headers.get("exceptionMessage", String.class);
                        String originalRequestJson = headers.get(IntegrationConfig.HEADER_REQUEST_JSON, String.class);
                        jsonContent = "Exception: " + exceptionMessage + "\nOriginal Request (if available): \n" + (originalRequestJson != null ? originalRequestJson : "N/A");
                        break;
                    default:
                        log.warn("Unknown logType in fileLoggingFlow: {}", logType);
                        jsonContent = "Unknown log type. Payload: " + payload;
                        break;
                }
                if (jsonContent != null) {
                    fileLoggerUtil.logJsonToFile(logType, requestId, jsonContent);
                } else {
                    log.warn("No JSON content found in headers for logType: {}", logType);
                }
            } else {
                log.warn("Message received on loggingChannel without logType header.");
            }
        }
    }
}


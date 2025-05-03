package com.example.trainservicecrewapi.service;

import com.example.trainservicecrewapi.model.StatusResponse;
import com.example.trainservicecrewapi.model.TrainServiceCrewRequest;
import com.example.trainservicecrewapi.model.TrainServiceCrewResponse;
import com.example.trainservicecrewapi.util.CsvReportUtil;
import com.example.trainservicecrewapi.util.FileLoggerUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Implementation of the TrainCrewService interface.
 * Handles the business logic for processing requests, logging, and reporting.
 */
@Service
public class TrainCrewServiceImpl implements TrainCrewService {

    private static final Logger log = LoggerFactory.getLogger(TrainCrewServiceImpl.class);

    private final FileLoggerUtil fileLoggerUtil;
    private final CsvReportUtil csvReportUtil;
    private final ObjectMapper objectMapper; // For converting objects to JSON for logging

    /**
     * Constructs the service implementation with necessary utility dependencies.
     *
     * @param fileLoggerUtil Utility for logging JSON to files.
     * @param csvReportUtil Utility for writing CSV report entries.
     * @param objectMapper Jackson ObjectMapper for JSON serialization.
     */
    @Autowired
    public TrainCrewServiceImpl(FileLoggerUtil fileLoggerUtil, CsvReportUtil csvReportUtil, ObjectMapper objectMapper) {
        this.fileLoggerUtil = fileLoggerUtil;
        this.csvReportUtil = csvReportUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TrainServiceCrewResponse processCrewRequest(TrainServiceCrewRequest request, String requestJson, LocalDateTime arrivalTime) {
        String requestId = request != null ? request.getRequestID() : "unknown";
        log.info("Processing crew request ID: {}", requestId);

        // 1. Log received request JSON
        fileLoggerUtil.logJsonToFile("received", requestId, requestJson);

        // 2. Create response object (transformation logic as per requirement 3)
        TrainServiceCrewResponse response = new TrainServiceCrewResponse(request);

        // 3. Log sent response JSON
        String responseJson = "";
        try {
            // Wrap the response list in the structure specified: {"TrainServiceCrewResponses": [...]} 
            Map<String, Object> responseWrapper = Collections.singletonMap("TrainServiceCrewResponses", Collections.singletonList(response));
            responseJson = objectMapper.writeValueAsString(responseWrapper);
            fileLoggerUtil.logJsonToFile("sent", requestId, responseJson);
        } catch (JsonProcessingException e) {
            log.error("Error serializing TrainServiceCrewResponse to JSON for request ID {}: {}", requestId, e.getMessage(), e);
            // Log error to file as well
            fileLoggerUtil.logJsonToFile("error", requestId, "{\"error\":\"Failed to serialize response\", \"detail\":\"" + e.getMessage() + "\"}");
            // Decide if we should still proceed or throw an exception
        }

        // 4. Write CSV report entry
        LocalDateTime sentTime = LocalDateTime.now();
        csvReportUtil.writeReportEntry(arrivalTime, requestId, "OK", sentTime, "CrewRequestEndpoint");

        log.info("Finished processing crew request ID: {}", requestId);
        return response; // Return the core response data, controller will wrap it
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StatusResponse processStatusUpdate(TrainServiceCrewResponse response, String responseJson, LocalDateTime arrivalTime) {
        String requestId = response != null ? response.getRequestID() : "unknown";
        log.info("Processing status update for request ID: {}", requestId);

        // 1. Log received response JSON (from first context)
        // Assuming the input JSON is the structure {"TrainServiceCrewResponses": [...]} 
        fileLoggerUtil.logJsonToFile("received", requestId, responseJson); // Log the raw input

        // 2. Create final status response object (requirement 6)
        StatusResponse statusResponse = new StatusResponse(response);

        // 3. Log sent status response JSON
        String statusResponseJson = "";
        try {
            // The final response is just the StatusResponse object directly
            statusResponseJson = objectMapper.writeValueAsString(statusResponse);
            fileLoggerUtil.logJsonToFile("sent", requestId, statusResponseJson);
        } catch (JsonProcessingException e) {
            log.error("Error serializing StatusResponse to JSON for request ID {}: {}", requestId, e.getMessage(), e);
            fileLoggerUtil.logJsonToFile("error", requestId, "{\"error\":\"Failed to serialize status response\", \"detail\":\"" + e.getMessage() + "\"}");
        }

        // 4. Write CSV report entry for the second context
        LocalDateTime sentTime = LocalDateTime.now();
        csvReportUtil.logStatusResponseReport(arrivalTime, statusResponse, sentTime);

        log.info("Finished processing status update for request ID: {}", requestId);
        return statusResponse;
    }
}


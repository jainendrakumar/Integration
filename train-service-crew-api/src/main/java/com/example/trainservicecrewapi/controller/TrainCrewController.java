package com.example.trainservicecrewapi.controller;

import com.example.trainservicecrewapi.model.StatusResponse;
import com.example.trainservicecrewapi.model.TrainServiceCrewRequest;
import com.example.trainservicecrewapi.model.TrainServiceCrewResponse;
import com.example.trainservicecrewapi.service.TrainCrewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling Train Service Crew requests.
 * Provides endpoints for receiving the initial request and the subsequent status update request.
 */
@RestController
@RequestMapping("/api/v1/train-crew")
public class TrainCrewController {

    private static final Logger log = LoggerFactory.getLogger(TrainCrewController.class);

    private final TrainCrewService trainCrewService;
    private final ObjectMapper objectMapper; // For logging raw request

    /**
     * Constructs the controller with the required service and ObjectMapper.
     *
     * @param trainCrewService The service handling business logic.
     * @param objectMapper Jackson ObjectMapper for JSON processing.
     */
    @Autowired
    public TrainCrewController(TrainCrewService trainCrewService, ObjectMapper objectMapper) {
        this.trainCrewService = trainCrewService;
        this.objectMapper = objectMapper;
    }

    /**
     * Endpoint to receive the initial Train Service Crew request (as per point 1).
     * Expects a JSON body like: {"TrainServiceCrewRequest": [{...request data...}]}
     *
     * @param requestMap The raw request map containing the "TrainServiceCrewRequest" key.
     * @return A ResponseEntity containing the processed TrainServiceCrewResponse wrapped as {"TrainServiceCrewResponses": [...]}.
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, List<TrainServiceCrewResponse>>> handleCrewRequest(@RequestBody Map<String, List<TrainServiceCrewRequest>> requestMap) {
        LocalDateTime arrivalTime = LocalDateTime.now();
        String rawJson = "";
        try {
            rawJson = objectMapper.writeValueAsString(requestMap);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize incoming request map to JSON for logging.");
            // Continue processing even if logging raw request fails initially
        }

        List<TrainServiceCrewRequest> requests = requestMap.get("TrainServiceCrewRequest");

        // Assuming the outer list always contains exactly one request object as per the example
        if (requests == null || requests.isEmpty()) {
            log.error("Received invalid request format: 'TrainServiceCrewRequest' key missing or empty array.");
            // Log the raw request if available
            if (!rawJson.isEmpty()) {
                trainCrewService.processCrewRequest(null, rawJson, arrivalTime); // Log error via service if possible
            }
            // Return 400 Bad Request as per requirement 7 (implicitly, for invalid structure)
            return ResponseEntity.badRequest().build();
        }

        // Process the first (and assumed only) request in the list
        TrainServiceCrewRequest request = requests.get(0);
        TrainServiceCrewResponse response = trainCrewService.processCrewRequest(request, rawJson, arrivalTime);

        // Wrap the response according to requirement 3: {"TrainServiceCrewResponses": [...]} 
        Map<String, List<TrainServiceCrewResponse>> responseWrapper = Collections.singletonMap("TrainServiceCrewResponses", Collections.singletonList(response));

        return ResponseEntity.ok(responseWrapper);
    }

    /**
     * Endpoint to receive the Train Service Crew response (from the first context) and return a status (as per point 5).
     * Expects a JSON body like: {"TrainServiceCrewResponses": [{...response data...}]}
     *
     * @param responseMap The raw request map containing the "TrainServiceCrewResponses" key.
     * @return A ResponseEntity containing the final StatusResponse.
     */
    @PostMapping("/status")
    public ResponseEntity<StatusResponse> handleStatusUpdate(@RequestBody Map<String, List<TrainServiceCrewResponse>> responseMap) {
        LocalDateTime arrivalTime = LocalDateTime.now();
        String rawJson = "";
        try {
            rawJson = objectMapper.writeValueAsString(responseMap);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize incoming status update map to JSON for logging.");
        }

        List<TrainServiceCrewResponse> responses = responseMap.get("TrainServiceCrewResponses");

        // Assuming the outer list always contains exactly one response object
        if (responses == null || responses.isEmpty()) {
            log.error("Received invalid status update format: 'TrainServiceCrewResponses' key missing or empty array.");
            if (!rawJson.isEmpty()) {
                 trainCrewService.processStatusUpdate(null, rawJson, arrivalTime); // Log error via service if possible
            }
            return ResponseEntity.badRequest().build();
        }

        // Process the first (and assumed only) response in the list
        TrainServiceCrewResponse response = responses.get(0);
        StatusResponse statusResponse = trainCrewService.processStatusUpdate(response, rawJson, arrivalTime);

        return ResponseEntity.ok(statusResponse);
    }

    // Consider adding a GlobalExceptionHandler for more robust error handling (e.g., JsonParseException)
}


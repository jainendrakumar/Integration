package com.example.trainservicecrewapi.service;

import com.example.trainservicecrewapi.model.StatusResponse;
import com.example.trainservicecrewapi.model.TrainServiceCrewRequest;
import com.example.trainservicecrewapi.model.TrainServiceCrewResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service interface defining the core business logic for processing train service crew requests.
 */
public interface TrainCrewService {

    /**
     * Processes the incoming train service crew request.
     * This involves logging the request, transforming it into a response,
     * logging the response, and recording the transaction details.
     *
     * @param request The deserialized TrainServiceCrewRequest object.
     * @param requestJson The raw JSON string of the request for logging.
     * @param arrivalTime The time the request arrived at the controller.
     * @return The generated TrainServiceCrewResponse object.
     */
    TrainServiceCrewResponse processCrewRequest(TrainServiceCrewRequest request, String requestJson, LocalDateTime arrivalTime);

    /**
     * Processes the incoming train service crew response (from the first endpoint/context)
     * to generate the final status response.
     * This involves logging the received response, creating the status response,
     * logging the status response, and recording the transaction details.
     *
     * @param response The deserialized TrainServiceCrewResponse object.
     * @param responseJson The raw JSON string of the response for logging.
     * @param arrivalTime The time the response arrived at the controller.
     * @return The generated StatusResponse object.
     */
    StatusResponse processStatusUpdate(TrainServiceCrewResponse response, String responseJson, LocalDateTime arrivalTime);
}


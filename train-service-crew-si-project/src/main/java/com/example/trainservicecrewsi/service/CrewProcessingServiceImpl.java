package com.example.trainservicecrewsi.service;

import com.example.trainservicecrewsi.model.StatusResponse;
import com.example.trainservicecrewsi.model.TrainServiceCrewRequest;
import com.example.trainservicecrewsi.model.TrainServiceCrewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

/**
 * Implementation of the CrewProcessingService.
 * Contains the core logic for transforming request/response objects.
 */
@Service
public class CrewProcessingServiceImpl implements CrewProcessingService {

    private static final Logger log = LoggerFactory.getLogger(CrewProcessingServiceImpl.class);

    /**
     * Processes the incoming TrainServiceCrewRequest and transforms it into a TrainServiceCrewResponse.
     * This method is designed to be called by a Spring Integration service activator.
     *
     * @param message The input message containing TrainServiceCrewRequest as payload.
     * @return The generated TrainServiceCrewResponse.
     */
    @Override
    public TrainServiceCrewResponse processCrewRequest(Message<TrainServiceCrewRequest> message) {
        TrainServiceCrewRequest request = message.getPayload();
        log.debug("Processing crew request ID: {}", request.getRequestID());

        // The core transformation logic is handled by the TrainServiceCrewResponse constructor
        TrainServiceCrewResponse response = new TrainServiceCrewResponse(request);

        log.debug("Finished processing crew request ID: {}", request.getRequestID());
        return response;
    }

    /**
     * Processes the incoming TrainServiceCrewResponse (from the first flow) and transforms it into a StatusResponse.
     * This method is designed to be called by a Spring Integration service activator.
     *
     * @param message The input message containing TrainServiceCrewResponse as payload.
     * @return The generated StatusResponse.
     */
    @Override
    public StatusResponse processStatusUpdate(Message<TrainServiceCrewResponse> message) {
        TrainServiceCrewResponse responsePayload = message.getPayload();
        log.debug("Processing status update for request ID: {}", responsePayload.getRequestID());

        // The core transformation logic is handled by the StatusResponse constructor
        StatusResponse statusResponse = new StatusResponse(responsePayload);

        log.debug("Finished processing status update for request ID: {}", responsePayload.getRequestID());
        return statusResponse;
    }
}


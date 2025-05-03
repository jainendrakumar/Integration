package com.example.trainservicecrewsi.service;

import com.example.trainservicecrewsi.model.StatusResponse;
import com.example.trainservicecrewsi.model.TrainServiceCrewRequest;
import com.example.trainservicecrewsi.model.TrainServiceCrewResponse;
import org.springframework.messaging.Message;

/**
 * Service interface for processing crew requests and status updates.
 * Used as a service activator target in Spring Integration flows.
 */
public interface CrewProcessingService {

    /**
     * Processes the incoming TrainServiceCrewRequest and transforms it into a TrainServiceCrewResponse.
     *
     * @param message The input message containing TrainServiceCrewRequest as payload.
     * @return The generated TrainServiceCrewResponse.
     */
    TrainServiceCrewResponse processCrewRequest(Message<TrainServiceCrewRequest> message);

    /**
     * Processes the incoming TrainServiceCrewResponse (from the first flow) and transforms it into a StatusResponse.
     *
     * @param message The input message containing TrainServiceCrewResponse as payload.
     * @return The generated StatusResponse.
     */
    StatusResponse processStatusUpdate(Message<TrainServiceCrewResponse> message);

}


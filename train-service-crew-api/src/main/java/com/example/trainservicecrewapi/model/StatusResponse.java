package com.example.trainservicecrewapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the final status response sent by the second endpoint.
 * This corresponds to the format specified in point 6 of the requirements.
 * It confirms the processing status for a given request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusResponse {

    /**
     * The identifier for the crew lobby, taken from the input request to the second endpoint.
     */
    private String CrewLobby;

    /**
     * The unique identifier for the request, taken from the input request to the second endpoint.
     */
    private String RequestID;

    /**
     * The processing status, typically "Processed".
     */
    private String STATUS = "Processed"; // Defaulting as per example

    /**
     * Constructor to create a StatusResponse from a TrainServiceCrewResponse.
     *
     * @param response The TrainServiceCrewResponse received by the second endpoint.
     */
    public StatusResponse(TrainServiceCrewResponse response) {
        this.CrewLobby = response.getCrewLobby();
        this.RequestID = response.getRequestID();
        // STATUS retains its default "Processed" value
    }
}


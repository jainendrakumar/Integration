package com.example.trainservicecrewapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the main response structure for the first processing endpoint.
 * This corresponds to the format specified in point 3 of the requirements.
 * It contains overall response details mirroring the request structure but with
 * simplified crew lobby details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainServiceCrewResponse {

    /**
     * The identifier for the crew lobby, taken from the request.
     */
    private String CrewLobby;

    /**
     * The unique identifier for the request, taken from the request.
     */
    private String RequestID;

    /**
     * A list containing simplified details for each crew lobby from the request.
     */
    private List<CrewLobbyDetailResponse> CrewLobbyDetails;

    /**
     * Constructor to create a response object from a request object.
     * Copies relevant header information and transforms the request details
     * into response details.
     *
     * @param request The corresponding TrainServiceCrewRequest object.
     */
    public TrainServiceCrewResponse(TrainServiceCrewRequest request) {
        this.CrewLobby = request.getCrewLobby();
        this.RequestID = request.getRequestID();
        if (request.getCrewLobbyDetails() != null) {
            this.CrewLobbyDetails = request.getCrewLobbyDetails().stream()
                                         .map(CrewLobbyDetailResponse::new) // Use constructor reference
                                         .collect(Collectors.toList());
        }
    }
}


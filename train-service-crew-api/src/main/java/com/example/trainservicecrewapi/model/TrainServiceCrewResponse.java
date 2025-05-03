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
@Data // Keep for toString, equals, hashCode, constructors
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
        this.CrewLobby = request.getCrewLobby(); // Uses explicit getter from TrainServiceCrewRequest
        this.RequestID = request.getRequestID(); // Uses explicit getter from TrainServiceCrewRequest
        if (request.getCrewLobbyDetails() != null) { // Uses explicit getter from TrainServiceCrewRequest
            this.CrewLobbyDetails = request.getCrewLobbyDetails().stream() // Uses explicit getter from TrainServiceCrewRequest
                    .map(CrewLobbyDetailResponse::new) // Use constructor reference
                    .collect(Collectors.toList());
        }
    }

    // --- Explicit Getter Methods ---

    public String getCrewLobby() {
        return CrewLobby;
    }

    public String getRequestID() {
        return RequestID;
    }

    public List<CrewLobbyDetailResponse> getCrewLobbyDetails() {
        return CrewLobbyDetails;
    }

    // --- Explicit Setter Methods ---

    public void setCrewLobby(String crewLobby) {
        CrewLobby = crewLobby;
    }

    public void setRequestID(String requestID) {
        RequestID = requestID;
    }

    public void setCrewLobbyDetails(List<CrewLobbyDetailResponse> crewLobbyDetails) {
        CrewLobbyDetails = crewLobbyDetails;
    }
}


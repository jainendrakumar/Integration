package com.example.trainservicecrewsi.model;

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
@Data // Using Lombok, but including explicit methods as fallback
@NoArgsConstructor
@AllArgsConstructor
public class TrainServiceCrewResponse {

    private String CrewLobby;
    private String RequestID;
    private List<CrewLobbyDetailResponse> CrewLobbyDetails;

    /**
     * Constructor to create a response object from a request object.
     * Copies relevant header information and transforms the request details
     * into response details.
     *
     * @param request The corresponding TrainServiceCrewRequest object.
     */
    public TrainServiceCrewResponse(TrainServiceCrewRequest request) {
        if (request != null) {
            this.CrewLobby = request.getCrewLobby(); // Uses explicit getter
            this.RequestID = request.getRequestID(); // Uses explicit getter
            if (request.getCrewLobbyDetails() != null) { // Uses explicit getter
                this.CrewLobbyDetails = request.getCrewLobbyDetails().stream() // Uses explicit getter
                                             .map(CrewLobbyDetailResponse::new) // Use constructor reference
                                             .collect(Collectors.toList());
            }
        }
    }

    // --- Explicit Getter Methods (Fallback) ---
    public String getCrewLobby() { return CrewLobby; }
    public String getRequestID() { return RequestID; }
    public List<CrewLobbyDetailResponse> getCrewLobbyDetails() { return CrewLobbyDetails; }

    // --- Explicit Setter Methods (Fallback) ---
    public void setCrewLobby(String crewLobby) { CrewLobby = crewLobby; }
    public void setRequestID(String requestID) { RequestID = requestID; }
    public void setCrewLobbyDetails(List<CrewLobbyDetailResponse> crewLobbyDetails) { CrewLobbyDetails = crewLobbyDetails; }
}


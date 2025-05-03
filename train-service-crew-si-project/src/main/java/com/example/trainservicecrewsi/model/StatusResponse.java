package com.example.trainservicecrewsi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the final status response sent by the second endpoint.
 * This corresponds to the format specified in point 6 of the requirements.
 * It confirms the processing status for a given request.
 */
@Data // Using Lombok, but including explicit methods as fallback
@NoArgsConstructor
@AllArgsConstructor
public class StatusResponse {

    private String CrewLobby;
    private String RequestID;
    private String STATUS = "Processed"; // Defaulting as per example

    /**
     * Constructor to create a StatusResponse from a TrainServiceCrewResponse.
     *
     * @param response The TrainServiceCrewResponse received by the second endpoint.
     */
    public StatusResponse(TrainServiceCrewResponse response) {
        if (response != null) {
            this.CrewLobby = response.getCrewLobby(); // Uses explicit getter
            this.RequestID = response.getRequestID(); // Uses explicit getter
            // STATUS retains its default "Processed" value
        }
    }

    // --- Explicit Getter Methods (Fallback) ---
    public String getCrewLobby() { return CrewLobby; }
    public String getRequestID() { return RequestID; }
    public String getSTATUS() { return STATUS; }

    // --- Explicit Setter Methods (Fallback) ---
    public void setCrewLobby(String crewLobby) { CrewLobby = crewLobby; }
    public void setRequestID(String requestID) { RequestID = requestID; }
    public void setSTATUS(String STATUS) { this.STATUS = STATUS; }
}


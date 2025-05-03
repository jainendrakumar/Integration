package com.example.trainservicecrewapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Represents the main request structure for the Train Service Crew API.
 * Contains overall request details and a list of crew lobby details.
 * This class represents the object *within* the "TrainServiceCrewRequest" array.
 */
@Data // Keep for toString, equals, hashCode, constructors
@NoArgsConstructor
@AllArgsConstructor
public class TrainServiceCrewRequest {

    /**
     * The identifier for the crew lobby (e.g., "DHN").
     */
    @JsonProperty("CrewLobby") // Explicit mapping
    private String CrewLobby;

    /**
     * The unique identifier for this request.
     */
    @JsonProperty("RequestID") // Explicit mapping
    private String RequestID;

    /**
     * Total number of Load IDs in the request.
     */
    @JsonProperty("TotNoOfLoadID") // Explicit mapping
    private int TotNoOfLoadID;

    /**
     * Total number of Crew Change Plan IDs in the request.
     */
    @JsonProperty("TotNoOfCrewChangePlanID") // Explicit mapping
    private int TotNoOfCrewChangePlanID;

    /**
     * A list containing detailed information for each crew lobby.
     */
    @JsonProperty("CrewLobbyDetails") // Explicit mapping
    private List<CrewLobbyDetailRequest> CrewLobbyDetails;

    // --- Explicit Getter Methods ---

    public String getCrewLobby() {
        return CrewLobby;
    }

    public String getRequestID() {
        return RequestID;
    }

    public int getTotNoOfLoadID() {
        return TotNoOfLoadID;
    }

    public int getTotNoOfCrewChangePlanID() {
        return TotNoOfCrewChangePlanID;
    }

    public List<CrewLobbyDetailRequest> getCrewLobbyDetails() {
        return CrewLobbyDetails;
    }

    // --- Explicit Setter Methods (Optional but good practice if needed elsewhere) ---

    public void setCrewLobby(String crewLobby) {
        CrewLobby = crewLobby;
    }

    public void setRequestID(String requestID) {
        RequestID = requestID;
    }

    public void setTotNoOfLoadID(int totNoOfLoadID) {
        TotNoOfLoadID = totNoOfLoadID;
    }

    public void setTotNoOfCrewChangePlanID(int totNoOfCrewChangePlanID) {
        TotNoOfCrewChangePlanID = totNoOfCrewChangePlanID;
    }

    public void setCrewLobbyDetails(List<CrewLobbyDetailRequest> crewLobbyDetails) {
        CrewLobbyDetails = crewLobbyDetails;
    }
}


package com.example.trainservicecrewsi.model;

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
@Data // Using Lombok, but including explicit methods as fallback
@NoArgsConstructor
@AllArgsConstructor
public class TrainServiceCrewRequest {

    @JsonProperty("CrewLobby")
    private String CrewLobby;

    @JsonProperty("RequestID")
    private String RequestID;

    @JsonProperty("TotNoOfLoadID")
    private int TotNoOfLoadID;

    @JsonProperty("TotNoOfCrewChangePlanID")
    private int TotNoOfCrewChangePlanID;

    @JsonProperty("CrewLobbyDetails")
    private List<CrewLobbyDetailRequest> CrewLobbyDetails;

    // --- Explicit Getter Methods (Fallback) ---
    public String getCrewLobby() { return CrewLobby; }
    public String getRequestID() { return RequestID; }
    public int getTotNoOfLoadID() { return TotNoOfLoadID; }
    public int getTotNoOfCrewChangePlanID() { return TotNoOfCrewChangePlanID; }
    public List<CrewLobbyDetailRequest> getCrewLobbyDetails() { return CrewLobbyDetails; }

    // --- Explicit Setter Methods (Fallback) ---
    public void setCrewLobby(String crewLobby) { CrewLobby = crewLobby; }
    public void setRequestID(String requestID) { RequestID = requestID; }
    public void setTotNoOfLoadID(int totNoOfLoadID) { TotNoOfLoadID = totNoOfLoadID; }
    public void setTotNoOfCrewChangePlanID(int totNoOfCrewChangePlanID) { TotNoOfCrewChangePlanID = totNoOfCrewChangePlanID; }
    public void setCrewLobbyDetails(List<CrewLobbyDetailRequest> crewLobbyDetails) { CrewLobbyDetails = crewLobbyDetails; }
}


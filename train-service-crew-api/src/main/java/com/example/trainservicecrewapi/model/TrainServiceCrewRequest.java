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
@Data
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
}


package com.example.trainservicecrewapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the detailed response information for a specific crew lobby detail.
 * This is part of the response structure defined in point 3 of the requirements.
 * It includes the LoadID, CrewChangePlanID, and status indicators.
 */
@Data // Keep for toString, equals, hashCode, constructors
@NoArgsConstructor
@AllArgsConstructor
public class CrewLobbyDetailResponse {
    /**
     * The identifier for the load, taken from the request.
     */
    private String LoadID;

    /**
     * The identifier for the crew change plan, taken from the request.
     */
    private String CrewChangePlanID;

    /**
     * Status indicator for the crew request pilot (e.g., "OK").
     * The exact meaning or generation logic for this status is not specified,
     * defaulting to "OK" as per the example.
     */
    private String crewreqpilot = "OK"; // Defaulting as per example

    /**
     * Status indicator for the crew guard (e.g., "OK").
     * The exact meaning or generation logic for this status is not specified,
     * defaulting to "OK" as per the example.
     */
    private String crewguard = "OK"; // Defaulting as per example

    /**
     * Status indicator for the role (e.g., "OK").
     * The exact meaning or generation logic for this status is not specified,
     * defaulting to "OK" as per the example.
     */
    private String role = "OK"; // Defaulting as per example

    /**
     * Constructor to easily create a response detail from request detail.
     * Copies relevant IDs and sets default status values.
     *
     * @param requestDetail The corresponding CrewLobbyDetailRequest object.
     */
    public CrewLobbyDetailResponse(CrewLobbyDetailRequest requestDetail) {
        this.LoadID = requestDetail.getLoadID(); // Uses explicit getter from CrewLobbyDetailRequest
        this.CrewChangePlanID = requestDetail.getCrewChangePlanID(); // Uses explicit getter from CrewLobbyDetailRequest
        // Status fields retain their default "OK" values
    }

    // --- Explicit Getter Methods ---

    public String getLoadID() {
        return LoadID;
    }

    public String getCrewChangePlanID() {
        return CrewChangePlanID;
    }

    public String getCrewreqpilot() {
        return crewreqpilot;
    }

    public String getCrewguard() {
        return crewguard;
    }

    public String getRole() {
        return role;
    }

    // --- Explicit Setter Methods ---

    public void setLoadID(String loadID) {
        LoadID = loadID;
    }

    public void setCrewChangePlanID(String crewChangePlanID) {
        CrewChangePlanID = crewChangePlanID;
    }

    public void setCrewreqpilot(String crewreqpilot) {
        this.crewreqpilot = crewreqpilot;
    }

    public void setCrewguard(String crewguard) {
        this.crewguard = crewguard;
    }

    public void setRole(String role) {
        this.role = role;
    }
}


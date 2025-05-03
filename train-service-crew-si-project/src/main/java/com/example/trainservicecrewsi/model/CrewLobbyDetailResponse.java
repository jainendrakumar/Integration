package com.example.trainservicecrewsi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the detailed response information for a specific crew lobby detail.
 * This is part of the response structure defined in point 3 of the requirements.
 * It includes the LoadID, CrewChangePlanID, and status indicators.
 */
@Data // Using Lombok, but including explicit methods as fallback
@NoArgsConstructor
@AllArgsConstructor
public class CrewLobbyDetailResponse {

    private String LoadID;
    private String CrewChangePlanID;
    private String crewreqpilot = "OK"; // Defaulting as per example
    private String crewguard = "OK"; // Defaulting as per example
    private String role = "OK"; // Defaulting as per example

    /**
     * Constructor to easily create a response detail from request detail.
     * Copies relevant IDs and sets default status values.
     *
     * @param requestDetail The corresponding CrewLobbyDetailRequest object.
     */
    public CrewLobbyDetailResponse(CrewLobbyDetailRequest requestDetail) {
        if (requestDetail != null) {
            this.LoadID = requestDetail.getLoadID(); // Uses explicit getter
            this.CrewChangePlanID = requestDetail.getCrewChangePlanID(); // Uses explicit getter
            // Status fields retain their default "OK" values
        }
    }

    // --- Explicit Getter Methods (Fallback) ---
    public String getLoadID() { return LoadID; }
    public String getCrewChangePlanID() { return CrewChangePlanID; }
    public String getCrewreqpilot() { return crewreqpilot; }
    public String getCrewguard() { return crewguard; }
    public String getRole() { return role; }

    // --- Explicit Setter Methods (Fallback) ---
    public void setLoadID(String loadID) { LoadID = loadID; }
    public void setCrewChangePlanID(String crewChangePlanID) { CrewChangePlanID = crewChangePlanID; }
    public void setCrewreqpilot(String crewreqpilot) { this.crewreqpilot = crewreqpilot; }
    public void setCrewguard(String crewguard) { this.crewguard = crewguard; }
    public void setRole(String role) { this.role = role; }
}


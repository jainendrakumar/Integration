package com.example.trainservicecrewsi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Represents the detailed information for a specific crew lobby within the request.
 * Contains load details, route information, crew requirements, and station sequences.
 */
@Data // Using Lombok, but including explicit methods as fallback
@NoArgsConstructor
@AllArgsConstructor
public class CrewLobbyDetailRequest {

    private String LoadID;
    private String LongHaulID;
    private String OriginStation;
    private String DestinationStation;

    @JsonProperty("From Station")
    private String FromStation; // Handles the key "From Station"

    private String ToStation;
    private String CrewChangePlanID;
    private Integer NrOfLPG;
    private Integer NrOfALP;
    private Integer NrOfGO;
    private Integer NrOfLPS; // Added based on the second object in the example
    private List<StationSequence> StationSequences;

    // --- Explicit Getter Methods (Fallback) ---
    public String getLoadID() { return LoadID; }
    public String getLongHaulID() { return LongHaulID; }
    public String getOriginStation() { return OriginStation; }
    public String getDestinationStation() { return DestinationStation; }
    public String getFromStation() { return FromStation; }
    public String getToStation() { return ToStation; }
    public String getCrewChangePlanID() { return CrewChangePlanID; }
    public Integer getNrOfLPG() { return NrOfLPG; }
    public Integer getNrOfALP() { return NrOfALP; }
    public Integer getNrOfGO() { return NrOfGO; }
    public Integer getNrOfLPS() { return NrOfLPS; }
    public List<StationSequence> getStationSequences() { return StationSequences; }

    // --- Explicit Setter Methods (Fallback) ---
    public void setLoadID(String loadID) { LoadID = loadID; }
    public void setLongHaulID(String longHaulID) { LongHaulID = longHaulID; }
    public void setOriginStation(String originStation) { OriginStation = originStation; }
    public void setDestinationStation(String destinationStation) { DestinationStation = destinationStation; }
    public void setFromStation(String fromStation) { FromStation = fromStation; }
    public void setToStation(String toStation) { ToStation = toStation; }
    public void setCrewChangePlanID(String crewChangePlanID) { CrewChangePlanID = crewChangePlanID; }
    public void setNrOfLPG(Integer nrOfLPG) { NrOfLPG = nrOfLPG; }
    public void setNrOfALP(Integer nrOfALP) { NrOfALP = nrOfALP; }
    public void setNrOfGO(Integer nrOfGO) { NrOfGO = nrOfGO; }
    public void setNrOfLPS(Integer nrOfLPS) { NrOfLPS = nrOfLPS; }
    public void setStationSequences(List<StationSequence> stationSequences) { StationSequences = stationSequences; }
}


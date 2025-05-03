package com.example.trainservicecrewapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Represents the detailed information for a specific crew lobby within the request.
 * Contains load details, route information, crew requirements, and station sequences.
 */
@Data // Keep for toString, equals, hashCode, constructors
@NoArgsConstructor
@AllArgsConstructor
public class CrewLobbyDetailRequest {
    /**
     * The identifier for the load.
     */
    private String LoadID;

    /**
     * The identifier for the long haul.
     */
    private String LongHaulID;

    /**
     * The origin station name.
     * Present in the first example object.
     */
    private String OriginStation;

    /**
     * The destination station name.
     * Present in the first example object.
     */
    private String DestinationStation;

    /**
     * The origin station name, potentially with a different key name.
     * Present in the second example object.
     * Uses Jackson annotation to map the JSON key "From Station" to this field.
     */
    @JsonProperty("From Station")
    private String FromStation; // Handles the key "From Station"

    /**
     * The destination station name, potentially with a different key name.
     * Present in the second example object.
     */
    private String ToStation;

    /**
     * The identifier for the crew change plan.
     */
    private String CrewChangePlanID;

    /**
     * Number of LPG (Loco Pilot Goods/Passenger - assumption, based on first object).
     * Note: The second example object uses "NrOfLPS". If both keys can appear,
     * this model might need adjustment (e.g., separate fields or a Map).
     * Assuming consistency or a typo for now, mapping based on the first object.
     */
    private Integer NrOfLPG;

    /**
     * Number of ALP (Assistant Loco Pilot).
     */
    private Integer NrOfALP;

    /**
     * Number of GO (Guard Officer - assumption).
     */
    private Integer NrOfGO;

    /**
     * Number of LPS (Loco Pilot Shunting? - assumption, based on second object).
     * Included to potentially capture the key from the second example object.
     * If "NrOfLPG" and "NrOfLPS" are mutually exclusive and represent the same concept,
     * consider using @JsonAlias or a custom deserializer.
     * For simplicity, including both as potentially optional fields.
     */
    private Integer NrOfLPS; // Added based on the second object in the example

    /**
     * A list of station sequences for this crew lobby detail.
     */
    private List<StationSequence> StationSequences;

    // --- Explicit Getter Methods ---

    public String getLoadID() {
        return LoadID;
    }

    public String getLongHaulID() {
        return LongHaulID;
    }

    public String getOriginStation() {
        return OriginStation;
    }

    public String getDestinationStation() {
        return DestinationStation;
    }

    public String getFromStation() {
        return FromStation;
    }

    public String getToStation() {
        return ToStation;
    }

    public String getCrewChangePlanID() {
        return CrewChangePlanID;
    }

    public Integer getNrOfLPG() {
        return NrOfLPG;
    }

    public Integer getNrOfALP() {
        return NrOfALP;
    }

    public Integer getNrOfGO() {
        return NrOfGO;
    }

    public Integer getNrOfLPS() {
        return NrOfLPS;
    }

    public List<StationSequence> getStationSequences() {
        return StationSequences;
    }

    // --- Explicit Setter Methods ---

    public void setLoadID(String loadID) {
        LoadID = loadID;
    }

    public void setLongHaulID(String longHaulID) {
        LongHaulID = longHaulID;
    }

    public void setOriginStation(String originStation) {
        OriginStation = originStation;
    }

    public void setDestinationStation(String destinationStation) {
        DestinationStation = destinationStation;
    }

    public void setFromStation(String fromStation) {
        FromStation = fromStation;
    }

    public void setToStation(String toStation) {
        ToStation = toStation;
    }

    public void setCrewChangePlanID(String crewChangePlanID) {
        CrewChangePlanID = crewChangePlanID;
    }

    public void setNrOfLPG(Integer nrOfLPG) {
        NrOfLPG = nrOfLPG;
    }

    public void setNrOfALP(Integer nrOfALP) {
        NrOfALP = nrOfALP;
    }

    public void setNrOfGO(Integer nrOfGO) {
        NrOfGO = nrOfGO;
    }

    public void setNrOfLPS(Integer nrOfLPS) {
        NrOfLPS = nrOfLPS;
    }

    public void setStationSequences(List<StationSequence> stationSequences) {
        StationSequences = stationSequences;
    }
}


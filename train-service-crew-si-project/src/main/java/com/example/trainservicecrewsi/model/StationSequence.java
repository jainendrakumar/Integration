package com.example.trainservicecrewsi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a single station sequence within crew lobby details.
 * Contains information about station code, sequence number, traction, ETA, and ETD.
 */
@Data // Using Lombok, but including explicit methods as fallback
@NoArgsConstructor
@AllArgsConstructor
public class StationSequence {
    /**
     * The code identifying the station.
     */
    private String StationCode;

    /**
     * The sequence number of the station in the route.
     */
    private int StationSeqNr;

    /**
     * The type of traction used (e.g., "ELEC").
     */
    private String Traction;

    /**
     * Estimated Time of Arrival (as a string, format might need clarification if parsing is required).
     */
    private String ETA;

    /**
     * Estimated Time of Departure (as a string, format might need clarification if parsing is required).
     */
    private String ETD;

    // --- Explicit Getter Methods (Fallback) ---
    public String getStationCode() { return StationCode; }
    public int getStationSeqNr() { return StationSeqNr; }
    public String getTraction() { return Traction; }
    public String getETA() { return ETA; }
    public String getETD() { return ETD; }

    // --- Explicit Setter Methods (Fallback) ---
    public void setStationCode(String stationCode) { StationCode = stationCode; }
    public void setStationSeqNr(int stationSeqNr) { StationSeqNr = stationSeqNr; }
    public void setTraction(String traction) { Traction = traction; }
    public void setETA(String ETA) { this.ETA = ETA; }
    public void setETD(String ETD) { this.ETD = ETD; }
}


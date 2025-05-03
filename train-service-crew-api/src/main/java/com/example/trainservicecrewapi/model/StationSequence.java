package com.example.trainservicecrewapi.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Represents a single station sequence within crew lobby details.
 * Contains information about station code, sequence number, traction, ETA, and ETD.
 */
@Data
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
}


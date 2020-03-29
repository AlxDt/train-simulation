package com.trainsimulation.model.core.environment.infrastructure.track;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.utility.TrainQueue;

// Segments are structures in a train line where trains move on; they connect train stations with other stations
public class Segment extends Track {
    // Represents the length of the segment (rounded to the nearest meter)
    private final int length;

    // Represents the trains (specifically, the train carriages) that are currently on this segment
    private final TrainQueue trainQueue;

    // Represents the name of the segment
    private String name;

    // Represents whether this segment is curved or not
    private boolean curved;

    // Represents the junction where it came from
    private Junction from;

    // Represents the junction where it goes to
    private Junction to;

    // Represents the station owner of this segment (null if none)
    private Station station;

    // Represents the platform hub this segment is on (null if none)
    private PlatformHub platformHub;

    Segment(TrainSystem trainSystem, int length) {
        super(trainSystem);

        this.length = length;
        this.curved = false;

        this.trainQueue = new TrainQueue();
    }

    public int getLength() {
        return length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Junction getFrom() {
        return from;
    }

    public void setFrom(Junction from) {
        this.from = from;
    }

    public Junction getTo() {
        return to;
    }

    public void setTo(Junction to) {
        this.to = to;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public TrainQueue getTrainQueue() {
        return trainQueue;
    }

    public PlatformHub getPlatformHub() {
        return platformHub;
    }

    public void setPlatformHub(PlatformHub platformHub) {
        this.platformHub = platformHub;
    }

    public boolean isCurved() {
        return curved;
    }

    public void setCurved(boolean curved) {
        this.curved = curved;
    }

    // Denotes the string identifier of this segment
    public static class SegmentIdentifier {
        // String constants for track indicators
        public static final String MAINLINE = "mainline";
        public static final String STATION = "station";
        public static final String LOOP = "end";
        public static final String DEPOT = "depot";

        public static final String DELIMITER = ":";

        public static final String DEPOT_IN = "in";
        public static final String DEPOT_OUT = "out";
        public static final String LOOP_IN = "into";
        public static final String LOOP_OUT = "out from";
    }
}

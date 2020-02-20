package com.trainsimulation.model.utility;

import com.trainsimulation.model.core.environment.infrastructure.track.Segment;

// Contains information about a train carriage's location
public class TrainCarriageLocation {
    // Denotes the distance this carriage has cleared
    private double segmentClearance;

    // Denotes where (segment) this carriage is located
    private Segment segmentLocation;

    public TrainCarriageLocation() {
    }

    public double getSegmentClearance() {
        return segmentClearance;
    }

    public void setSegmentClearance(double segmentClearance) {
        this.segmentClearance = segmentClearance;
    }

    public Segment getSegmentLocation() {
        return segmentLocation;
    }

    public void setSegmentLocation(Segment segmentLocation) {
        this.segmentLocation = segmentLocation;
    }
}

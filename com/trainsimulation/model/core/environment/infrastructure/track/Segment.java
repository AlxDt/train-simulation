package com.trainsimulation.model.core.environment.infrastructure.track;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.TrainCarriage;

import java.util.concurrent.ConcurrentLinkedDeque;

// Segments are structures in a train line where trains move on; they connect train stations with other stations
public class Segment extends Track {
    // Represents the length of the segment (rounded to the nearest meter)
    private final int length;

    // Represents the trains (specifically, the train carriages) that are currently on this segment
    private final ConcurrentLinkedDeque<TrainCarriage> trainQueue;

    // Represents the name of the segment
    private String name;

    // Represents whether this segment is curved or not
    private boolean curved;

    // Represents the segment connector where it goes to
    private Junction to;

    // Represents the station owner of this segment (null if none)
    private Station station;

    // Represents the platform hub this segment is on (null if none)
    private PlatformHub platformHub;

//    public Segment(Segment segment) {
//        super(segment.getTrainSystem());
//
//        this.length = segment.getLength();
//        this.trainQueue = segment.getTrainQueue();
//        this.name = segment.getName();
//        this.curved = segment.isCurved();
//        this.to = segment.getTo();
//        this.station = segment.getStation();
//        this.platformHub = segment.getPlatformHub();
//    }

    Segment(TrainSystem trainSystem, int length) {
        super(trainSystem);

        this.length = length;
        this.curved = false;

        this.trainQueue = new ConcurrentLinkedDeque<>();
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

    public ConcurrentLinkedDeque<TrainCarriage> getTrainQueue() {
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
}

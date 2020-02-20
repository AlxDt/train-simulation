package com.trainsimulation.model.core.environment.infrastructure.track;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

// Junction connect segments with each other
public class Junction extends Track {

    // Projects the segment(s) coming out of this connector
    private final List<Segment> outSegments;

    // Acts as a "stoplight" for trains
    private final Semaphore signal;

    // Denotes whether the junction is the end of the train line or not
    private boolean end;

    public Junction(TrainSystem trainSystem, List<Segment> outSegments) {
        super(trainSystem);

        this.outSegments = outSegments;
        this.end = false;
        this.signal = new Semaphore(1, true);
    }

    public Junction(TrainSystem trainSystem) {
        super(trainSystem);

        this.outSegments = new ArrayList<>();
        this.end = false;
        this.signal = new Semaphore(1, true);
    }

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public Semaphore getSignal() {
        return signal;
    }

    public List<Segment> getOutSegments() {
        return outSegments;
    }
}

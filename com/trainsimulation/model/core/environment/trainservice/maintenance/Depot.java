package com.trainsimulation.model.core.environment.trainservice.maintenance;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.PlatformHub;

// A depot is where the trains of a train system originate
public class Depot extends Maintenance {
    // Contains the connections points of this depot to the train line
    private final PlatformHub platformHub;

    public Depot(TrainSystem trainSystem) {
        super(trainSystem);

        // The length of the depot platform is arbitrary because the depot is just seen as the spawning and de-spawning
        // point of trains
        final int ARBITRARY_DEPOT_LENGTH = 100;

        this.platformHub = new PlatformHub(trainSystem, ARBITRARY_DEPOT_LENGTH);
    }

    public PlatformHub getPlatformHub() {
        return platformHub;
    }
}

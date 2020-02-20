package com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.PlatformHub;
import com.trainsimulation.model.db.entity.PlatformsEntity;
import com.trainsimulation.model.utility.Capacity;
import com.trainsimulation.model.utility.Schedule;

import java.util.List;
import java.util.Objects;

// Represents a station's platform in a single direction
public class Platform extends StationSet {
    // TODO: Transfer these constants to the database
    public static final int LRT_1_PLATFORM_LENGTH = 106;
    public static final int LRT_2_PLATFORM_LENGTH = 90;
    public static final int MRT_3_PLATFORM_LENGTH = 99;

    // A list of time ranges which denotes the intervals which are considered peak hours
    private final List<Schedule> peakHoursRange;

    // A list of time ranges which denotes the intervals which signify when the inflow rates are to be checked
    private final List<Schedule> checkInflowRateInterval;

    // Connects the platform to the train system; it also allows trains to have a place to stop at the stations
    private final PlatformHub platformHub;

    // Denotes the capacity of the station's platforms (assuming that both of the platforms are of the same size)
    private final Capacity platformCapacity;

    // Denotes whether the station accepts the loading and unloading of passengers
    private boolean operational;

    // Denotes whether the station may be passed through by a train
    private boolean passable;

    public Platform(TrainSystem trainSystem, List<Schedule> peakHoursRange,
                    List<Schedule> checkInflowRateInterval, Capacity platformCapacity, boolean operational,
                    boolean passable, final int platformLength) {
        super(trainSystem);

        this.peakHoursRange = peakHoursRange;
        this.checkInflowRateInterval = checkInflowRateInterval;
        this.platformCapacity = platformCapacity;
        this.operational = operational;
        this.passable = passable;

        this.platformHub = new PlatformHub(trainSystem, platformLength);
    }

    public Platform(TrainSystem trainSystem, PlatformsEntity platformsEntity, final int platformLength) {
        super(trainSystem);

        // TODO: Implement peak hours and check inflow rate functionality
        this.peakHoursRange = null;
        this.checkInflowRateInterval = null;
        this.platformCapacity = new Capacity(platformsEntity.getPlatformCapacity());
        this.operational = platformsEntity.getOperational() == 1;
        this.passable = platformsEntity.getOperational() == 1;

        this.platformHub = new PlatformHub(trainSystem, platformLength);
    }

    // FIXME: Temporary constructor - refactor immediately with non-null values from the database
    public Platform(TrainSystem trainSystem, final int platformLength) {
        super(trainSystem);

        this.peakHoursRange = null;
        this.checkInflowRateInterval = null;
        this.platformCapacity = null;
        this.operational = true;
        this.passable = true;

        this.platformHub = new PlatformHub(trainSystem, platformLength);
    }

    public List<Schedule> getPeakHoursRange() {
        return peakHoursRange;
    }

    public List<Schedule> getCheckInflowRateInterval() {
        return checkInflowRateInterval;
    }

    public PlatformHub getPlatformHub() {
        return platformHub;
    }

    public Capacity getPlatformCapacity() {
        return platformCapacity;
    }

    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public boolean isPassable() {
        return passable;
    }

    public void setPassable(boolean passable) {
        this.passable = passable;
    }
}

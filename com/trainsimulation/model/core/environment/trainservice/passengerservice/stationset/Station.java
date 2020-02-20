package com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Track;
import com.trainsimulation.model.db.entity.StationsEntity;
import com.trainsimulation.model.simulator.SimulationTime;
import com.trainsimulation.model.utility.Schedule;
import com.trainsimulation.model.utility.StationCapacity;

import java.util.EnumMap;
import java.util.Map;

// Stations are structures in a train line where trains regularly stop to load and unload passengers
public class Station extends StationSet {
    // Represents the name of the station
    private final String name;

    // Represents the sequence number of this station
    private final int sequence;

    // Represents the operating schedule of the station
    private final Schedule operatingHours;

    // Represents the passenger capacity specifics of the station
    private final StationCapacity capacity;

    // Represents each of the station's platforms in both directions
    private final Map<Track.Direction, Platform> platforms;

    // Represents this station's distance to the previous station, according to its sequence
    private final int distanceToPrevious;

    public Station(Station station) {
        super(station.getTrainSystem());

        this.name = station.getName();
        this.sequence = station.getSequence();
        this.operatingHours = new Schedule(station.getOperatingHours());
        this.capacity = new StationCapacity(station.getCapacity());
        this.platforms = new EnumMap<>(station.getPlatforms());
        this.distanceToPrevious = station.getDistanceToPrevious();
    }

    public Station(TrainSystem trainSystem, StationsEntity stationsEntity) {
        super(trainSystem);

        // TODO: Fix nulls
        this.name = stationsEntity.getName();
        this.sequence = stationsEntity.getSequence();
        this.operatingHours = new Schedule(stationsEntity.getSchedulesByOperatingHours());
        this.capacity = new StationCapacity(stationsEntity.getStationCapacitiesByStationCapacities());
        this.distanceToPrevious = stationsEntity.getDistanceToPrevious();

        // Set the platforms up
        this.platforms = new EnumMap<>(Track.Direction.class);

        this.platforms.put(Track.Direction.NORTHBOUND, new Platform(trainSystem, Platform.LRT_2_PLATFORM_LENGTH));

        this.platforms.put(Track.Direction.SOUTHBOUND, new Platform(trainSystem, Platform.LRT_2_PLATFORM_LENGTH));
    }

    public String getName() {
        return name;
    }

    public int getSequence() {
        return sequence;
    }

    public Schedule getOperatingHours() {
        return operatingHours;
    }

    public StationCapacity getCapacity() {
        return capacity;
    }

    public Map<Track.Direction, Platform> getPlatforms() {
        return platforms;
    }

    public int getDistanceToPrevious() {
        return distanceToPrevious;
    }

    // Checks the time, inflow rate, current number of passengers in concourse and platform areas, and operational
    // status; tells the gates to temporarily stop the flow of passengers
    public void activateCrowdControl(SimulationTime time, double inflowRate, StationCapacity capacity,
                                     boolean operational) {
        // TODO: Implement crowd control logic
    }

    // Designates the station as operational and passable depending on time
    public void openStation(SimulationTime time) {
        // TODO: Implement station opening logic
    }

    // Designates the station as operational and passable
    public void openStation() {
        // TODO: Implement station opening logic
    }

    // Designates the station as not operational due to the time with the value of passable depending on the parameter
    public void closeStation(SimulationTime time, boolean passable) {
        // TODO: Implement station closing logic
    }

    // Designates the station as not operational with the value of passable depending on the parameter
    public void closeStation(boolean passable) {
        // TODO: Implement station closing logic
    }
}

package com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset;

import com.crowdsimulation.controller.controls.feature.main.MainScreenController;
import com.crowdsimulation.model.core.agent.passenger.movement.PassengerTripInformation;
import com.crowdsimulation.model.core.environment.station.Floor;
import com.crowdsimulation.model.core.environment.station.patch.patchobject.passable.gate.StationGate;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Track;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.property.StationProperty;
import com.trainsimulation.model.db.entity.StationsEntity;
import com.trainsimulation.model.simulator.SimulationTime;
import com.trainsimulation.model.utility.Schedule;
import com.trainsimulation.model.utility.StationCapacity;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Stations are structures in a train line where trains regularly stop to load and unload passengers
public class Station extends StationSet {
    // Sort by the sequence number of the station from the depot in ascending order
    public static Comparator<Station> StationByDepotSequenceAscending = new Comparator<Station>() {
        @Override
        public int compare(Station o1, Station o2) {
            return Integer.compare(o1.depotSequence, o2.depotSequence);
        }
    };

    // Sort by the sequence number of the station from the depot in descending order
    public static Comparator<Station> StationByDepotSequenceDescending = new Comparator<Station>() {
        @Override
        public int compare(Station o1, Station o2) {
            return Integer.compare(o2.depotSequence, o1.depotSequence);
        }
    };

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

    // Represents this station's chronological order from the depot, according to its sequence
    private final int depotSequence;

    // Represents a summarized version of this station
    private final StationProperty stationProperty;

    // Denotes the train system of this station
    private final TrainSystem trainSystem;

    // Denotes the file path where the files for this train system is found
    private final String stationPath;

    // Represents the physical layout of the station with all the station objects and floors
    private com.crowdsimulation.model.core.environment.station.Station stationLayout;

    // TODO: Offload to station gate itself
    // Denotes the backlogs of the station gates of this station
    private final HashMap<StationGate, List<PassengerTripInformation>> passengerBacklogs;

    // Initialize a thread pool to run floors in parallel
    private ExecutorService floorExecutorService;

    public Station(TrainSystem trainSystem, StationsEntity stationsEntity) {
        super(trainSystem);

        // TODO: Fix nulls
        this.name = stationsEntity.getName();
        this.sequence = stationsEntity.getSequence();
        this.operatingHours = new Schedule(stationsEntity.getSchedulesByOperatingHours());
        this.capacity = new StationCapacity(stationsEntity.getStationCapacitiesByStationCapacities());
        this.distanceToPrevious = stationsEntity.getDistanceToPrevious();
        this.depotSequence = stationsEntity.getDepotSequence();

        // Set the platforms up
        this.platforms = new EnumMap<>(Track.Direction.class);

        // TODO: Retrieve from the database instead of using the LRT-1 platform length for all stations
        this.platforms.put(Track.Direction.NORTHBOUND, new Platform(trainSystem, Platform.LRT_1_PLATFORM_LENGTH,
                Track.Direction.NORTHBOUND));

        this.platforms.put(Track.Direction.SOUTHBOUND, new Platform(trainSystem, Platform.LRT_1_PLATFORM_LENGTH,
                Track.Direction.SOUTHBOUND));

        this.stationProperty = new StationProperty(this);

        this.trainSystem = trainSystem;

        this.stationPath = trainSystem.getTrainSystemInformation().getTrainSystemPath() + "\\stations\\" + this.name;
        this.stationLayout = null;

        // TODO: Move to station gate itself
        this.passengerBacklogs = new HashMap<>();
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

    public StationProperty getStationProperty() {
        return stationProperty;
    }

    public String getStationPath() {
        return stationPath;
    }

    public com.crowdsimulation.model.core.environment.station.Station getStationLayout() {
        return stationLayout;
    }

    public void setStationLayout(com.crowdsimulation.model.core.environment.station.Station stationLayout) {
        this.stationLayout = stationLayout;

        // TODO: Offload to station gate itself
        for (Floor floor : this.stationLayout.getFloors()) {
            for (StationGate stationGate : floor.getStationGates()) {
                this.passengerBacklogs.put(stationGate, new ArrayList<>());
            }
        }
    }

    public int getDepotSequence() {
        return depotSequence;
    }

    @Override
    public TrainSystem getTrainSystem() {
        return trainSystem;
    }

    public ExecutorService getFloorExecutorService() {
        return floorExecutorService;
    }

    public void setFloorExecutorService(int threads) {
        this.floorExecutorService = Executors.newFixedThreadPool(threads);
    }

    public HashMap<StationGate, List<PassengerTripInformation>> getPassengerBacklogs() {
        return passengerBacklogs;
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

    // Contains the necessary operations to load a station in parallel
    public static class StationLayoutLoadTask implements Callable<Void> {
        private final Station station;

        public StationLayoutLoadTask(Station station) {
            this.station = station;
        }

        @Override
        public Void call() throws Exception {
            // Prepare the path where the station layout may be found
            String stationLayoutPath
                    = station.getStationPath() + "\\run\\"
                    + station.getName()
                    + com.crowdsimulation.model.core.environment.station.Station.STATION_LAYOUT_FILE_EXTENSION;

            File stationLayoutFile = new File(stationLayoutPath);

            // Load the station layout from the file
            final com.crowdsimulation.model.core.environment.station.Station stationLayout
                    = MainScreenController.loadStation(stationLayoutFile);

            // Then set its parent
            station.setStationLayout(stationLayout);

            // Set the thread pool of this station with the number of threads as the number of floors in the station
            station.setFloorExecutorService(stationLayout.getFloors().size());

            System.out.println("Successfully loaded " + stationLayoutFile.getName());

            return null;
        }
    }
}

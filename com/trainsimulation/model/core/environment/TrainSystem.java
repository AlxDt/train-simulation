package com.trainsimulation.model.core.environment;

import com.trainsimulation.model.core.agent.passenger.Passenger;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.utility.TrainSystemInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Denotes a train system object containing all its information, depot, and stations
public class TrainSystem {
    // Denotes basic information about this train system
    private final TrainSystemInformation trainSystemInformation;

    // Denotes the stations of this train system
    private final List<Station> stations;

    // Contains all the inactive trains in the simulation
    private final List<Train> inactiveTrains;

    // Contains all the active trains in the simulation
    private final List<Train> activeTrains;

    // Contains all passengers in the simulation
    private final List<Passenger> passengers;

    // Denotes the depot of this train system
    private Depot depot;

    public TrainSystem(TrainSystemInformation trainSystemInformation) {
        this.trainSystemInformation = trainSystemInformation;
        this.stations = new ArrayList<>();
        this.inactiveTrains = new ArrayList<>();
        this.activeTrains = new ArrayList<>();
        this.passengers = new ArrayList<>();
        this.depot = null;
    }

    public TrainSystem(TrainSystemInformation trainSystemInformation, Depot depot) {
        this.trainSystemInformation = trainSystemInformation;
        this.stations = new ArrayList<>();
        this.inactiveTrains = new ArrayList<>();
        this.activeTrains = new ArrayList<>();
        this.passengers = new ArrayList<>();
        this.depot = depot;
    }

    public TrainSystemInformation getTrainSystemInformation() {
        return trainSystemInformation;
    }

    public List<Station> getStations() {
        return stations;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public List<Train> getInactiveTrains() {
        return inactiveTrains;
    }

    public List<Train> getActiveTrains() {
        return activeTrains;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    // Initialize station layouts of all stations in parallel
    public void initializeStationLayouts(List<Station> stations) {
        // Initialize a thread pool to load station layouts in parallel
        final int NUM_CPUS = Runtime.getRuntime().availableProcessors();
        ExecutorService stationLayoutLoadingExecutorService = Executors.newFixedThreadPool(NUM_CPUS);

        List<Station.StationLayoutLoadTask> stationLayoutLoadTasks = new ArrayList<>();

        for (Station station : stations) {
            stationLayoutLoadTasks.add(new Station.StationLayoutLoadTask(station));
        }

        try {
            stationLayoutLoadingExecutorService.invokeAll(stationLayoutLoadTasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrainSystem that = (TrainSystem) o;

        return trainSystemInformation.equals(that.trainSystemInformation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trainSystemInformation);
    }
}

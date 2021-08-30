package com.trainsimulation.model.core.environment;

import com.crowdsimulation.model.core.agent.passenger.movement.RoutePlan;
import com.trainsimulation.model.core.agent.passenger.Passenger;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.simulator.SimulationTime;
import com.trainsimulation.model.utility.TrainSystemInformation;

import java.util.*;
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

    // Denotes the passenger list of this train system (the list of passengers to be spawned by the simulator, and the
    // time
    private final List<RoutePlan.PassengerTripInformation> passengerList;

    public TrainSystem(TrainSystemInformation trainSystemInformation) {
        this.trainSystemInformation = trainSystemInformation;
        this.stations = new ArrayList<>();
        this.inactiveTrains = new ArrayList<>();
        this.activeTrains = new ArrayList<>();
        this.passengers = new ArrayList<>();
        this.depot = null;
        this.passengerList = new ArrayList<>();
    }

    public TrainSystem(TrainSystemInformation trainSystemInformation, Depot depot) {
        this.trainSystemInformation = trainSystemInformation;
        this.stations = new ArrayList<>();
        this.inactiveTrains = new ArrayList<>();
        this.activeTrains = new ArrayList<>();
        this.passengers = new ArrayList<>();
        this.depot = depot;
        this.passengerList = new ArrayList<>();
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

    // Retrieve a station using its name
    public Station retrieveStation(String stationName) {
        // Correct differences between the station names in the database and the station names in the passenger list
        stationName = fixDiscrepancy(stationName);

        for (Station station : this.stations) {
            if (station.getName().equals(stationName)) {
                return station;
            }
        }

        return null;
    }

    // Fix known discrepancies between the station name in the database versus in the passenger list
    private String fixDiscrepancy(String stationName) {
        if (stationName.equals("V. Cruz")) {
            stationName = "Vito Cruz";
        } else if (stationName.equals("A. Santos")) {
            stationName = "Abad Santos";
        } else if (stationName.equals("Edsa")) {
            stationName = "EDSA";
        } else if (stationName.equals("P. Quirino")) {
            stationName = "Quirino";
        } else if (stationName.equals("P. Gil")) {
            stationName = "Pedro Gil";
        } else if (stationName.equals("Monumento")) {
            stationName = "Yamaha Monumento";
        } else if (stationName.equals("Boni")) {
            stationName = "Boni Avenue";
        } else if (stationName.equals("Shaw Blvd")) {
            stationName = "Shaw Boulevard";
        } else if (stationName.equals("Araneta-Cubao")) {
            stationName = "Cubao";
        } else if (stationName.equals("Cubao")) {
            stationName = "Araneta Center-Cubao";
        } else if (stationName.equals("Betty Go")) {
            stationName = "Betty-Go Belmonte";
        }

        return stationName;
    }

    public void loadPassengerList(List<RoutePlan.PassengerTripInformation> passengerList) {
        this.passengerList.clear();
        this.passengerList.addAll(passengerList);
    }

    // Remove all trips that happen before the simulation starts
    public void removeTripsBeforeStartTime(SimulationTime simulationTime) {
        List<RoutePlan.PassengerTripInformation> passengerTripsToRemove = new ArrayList<>();

        for (RoutePlan.PassengerTripInformation passengerTripInformation : this.passengerList) {
            if (passengerTripInformation.getEntryTime().isBefore(simulationTime.getStartTime())) {
                passengerTripsToRemove.add(passengerTripInformation);
            } else {
                break;
            }
        }

        this.passengerList.removeAll(passengerTripsToRemove);
    }

    // Collects all passengers to be spawned in the next tick, given the current time
    public HashMap<Station, List<RoutePlan.PassengerTripInformation>> getPassengersToSpawn(
            SimulationTime simulationTime
    ) {
        HashMap<Station, List<RoutePlan.PassengerTripInformation>> stationListHashMap = new HashMap<>();
        List<RoutePlan.PassengerTripInformation> passengerTripsDone = new ArrayList<>();

        for (RoutePlan.PassengerTripInformation passengerTripInformation : this.passengerList) {
            if (passengerTripInformation.getEntryTime().equals(simulationTime.getTime())) {
                if (stationListHashMap.get(passengerTripInformation.getEntryStation()) == null) {
                    stationListHashMap.put(
                            passengerTripInformation.getEntryStation(),
                            Collections.synchronizedList(new ArrayList<>())
                    );
                }

                stationListHashMap.get(passengerTripInformation.getEntryStation()).add(passengerTripInformation);

                passengerTripsDone.add(passengerTripInformation);
            } else if (passengerTripInformation.getEntryTime().isAfter(simulationTime.getTime())) {
                break;
            }
        }

        this.passengerList.removeAll(passengerTripsDone);

        return stationListHashMap;
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

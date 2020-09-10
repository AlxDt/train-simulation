package com.trainsimulation.model.simulator.setup;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Junction;
import com.trainsimulation.model.core.environment.infrastructure.track.PlatformHub;
import com.trainsimulation.model.core.environment.infrastructure.track.Track;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.db.DatabaseInterface;
import com.trainsimulation.model.db.DatabaseQueries;
import com.trainsimulation.model.db.entity.EndSegmentsEntity;
import com.trainsimulation.model.utility.TrainSystemInformation;

import java.util.ArrayList;
import java.util.List;

// A class used to set the environment of the simulation
public class EnvironmentSetup {
    // Set the environment of the given train station up
    public static List<TrainSystem> setup(DatabaseInterface databaseInterface) {
        // Take note of each train system
        List<TrainSystem> trainSystems = new ArrayList<>();

        // Retrieve all train system information
        List<TrainSystemInformation> trainSystemInformations = DatabaseQueries.getTrainSystems(databaseInterface);

        // From each of the retrieved information about the train system, realize and create such train system
        for (TrainSystemInformation trainSystemInformation : trainSystemInformations) {
            switch (trainSystemInformation.getName()) {
                case "LRT-1":
                    trainSystems.add(setupLRT1(databaseInterface, trainSystemInformation));

                    break;
                case "LRT-2":
                    trainSystems.add(setupLRT2(databaseInterface, trainSystemInformation));

                    break;
                case "MRT-3":
                    trainSystems.add(setupMRT3(databaseInterface, trainSystemInformation));

                    break;
            }
        }

        return trainSystems;
    }

    private static TrainSystem setupLRT1(DatabaseInterface databaseInterface,
                                         TrainSystemInformation trainSystemInformation) {
        // Create a train system object
        TrainSystem trainSystem = new TrainSystem(trainSystemInformation);

        // Retrieve all stations
        List<Station> stations = DatabaseQueries.getStations(databaseInterface, trainSystem);

        // Add each to this train system's record
        trainSystem.getStations().addAll(stations);

        // Connect all stations
        for (int stationIndex = 1; stationIndex < stations.size(); stationIndex++) {
            // Get the preceding station
            Station precedingStation = stations.get(stationIndex - 1);

            // Get the current station
            Station currentStation = stations.get(stationIndex);

            // Make sure that the sequence of the stations are correct
            assert currentStation.getSequence() > precedingStation.getSequence() : "Stations are out of sequence";

            // Connect the two together
            Track.connectStations(precedingStation, currentStation, currentStation.getDistanceToPrevious());
        }

        // Get information about the train line's edge segments
        List<EndSegmentsEntity> endSegmentsEntities = DatabaseQueries.getEndSegmentInformation(databaseInterface,
                trainSystemInformation);

        int northEndSegmentLength = endSegmentsEntities.get(1).getLength();
        int southEndSegmentLength = endSegmentsEntities.get(0).getLength();

        // "Close" the loop of the two edge stations
        Station northEdgeStation = stations.get(stations.size() - 1);
        Station southEdgeStation = stations.get(0);

        Track.formNorthLoop(northEdgeStation, northEndSegmentLength);
        Track.formSouthLoop(southEdgeStation, southEndSegmentLength);

        // Mark all segments which belong to a station as belonging to that station
        PlatformHub currentPlatformHub;

        for (Station station : stations) {
            currentPlatformHub = station.getPlatforms().get(Track.Direction.NORTHBOUND).getPlatformHub();

            currentPlatformHub.getPlatformSegment().setStation(station);
            currentPlatformHub.getPlatformSegment().setPlatformHub(currentPlatformHub);
            currentPlatformHub.getPlatformSegment().setDirection(Track.Direction.NORTHBOUND);

            currentPlatformHub = station.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub();

            currentPlatformHub.getPlatformSegment().setStation(station);
            currentPlatformHub.getPlatformSegment().setPlatformHub(currentPlatformHub);
            currentPlatformHub.getPlatformSegment().setDirection(Track.Direction.SOUTHBOUND);
        }

        // Get the junctions where the depot is to be connected
        // The depot is to be connected to Baclaran station - the northernmost station in the system
        Station depotStation = stations.get(stations.size() - 1);

        Junction inJunction = depotStation.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub()
                .getInConnector();
        Junction outJunction = depotStation.getPlatforms().get(Track.Direction.NORTHBOUND).getPlatformHub()
                .getOutConnector();

        // Get the length of the spur segment which connects to the depot
        int spurDepotSegmentLength = DatabaseQueries.getSpurDepotSegmentLength(databaseInterface,
                trainSystemInformation);

        // Create the depot object, then connect it to the rest of the constructed train line
        Depot depot = new Depot(trainSystem);
        Track.connectDepot(depot, inJunction, outJunction, spurDepotSegmentLength);

        // Set the newly connected depot of the train system
        trainSystem.setDepot(depot);

        // Return the newly created train system
        return trainSystem;
    }

    // Set the LRT-2 system up
    private static TrainSystem setupLRT2(DatabaseInterface databaseInterface,
                                         TrainSystemInformation trainSystemInformation) {
        // Create a train system object
        TrainSystem trainSystem = new TrainSystem(trainSystemInformation);

        // Retrieve all stations
        List<Station> stations = DatabaseQueries.getStations(databaseInterface, trainSystem);

        // Add each to this train system's record
        trainSystem.getStations().addAll(stations);

        // Connect all stations
        for (int stationIndex = 1; stationIndex < stations.size(); stationIndex++) {
            // Get the preceding station
            Station precedingStation = stations.get(stationIndex - 1);

            // Get the current station
            Station currentStation = stations.get(stationIndex);

            // Make sure that the sequence of the stations are correct
            assert currentStation.getSequence() > precedingStation.getSequence() : "Stations are out of sequence";

            // Connect the two together
            Track.connectStations(precedingStation, currentStation, currentStation.getDistanceToPrevious());
        }

        // Get information about the train line's edge segments
        List<EndSegmentsEntity> endSegmentsEntities = DatabaseQueries.getEndSegmentInformation(databaseInterface,
                trainSystemInformation);

        int northEndSegmentLength = endSegmentsEntities.get(1).getLength();
        int southEndSegmentLength = endSegmentsEntities.get(0).getLength();

        // "Close" the loop of the two edge stations
        Station northEdgeStation = stations.get(stations.size() - 1);
        Station southEdgeStation = stations.get(0);

        Track.formNorthLoop(northEdgeStation, northEndSegmentLength);
        Track.formSouthLoop(southEdgeStation, southEndSegmentLength);

        // Mark all segments which belong to a station as belonging to that station
        PlatformHub currentPlatformHub;

        for (Station station : stations) {
            currentPlatformHub = station.getPlatforms().get(Track.Direction.NORTHBOUND).getPlatformHub();

            currentPlatformHub.getPlatformSegment().setStation(station);
            currentPlatformHub.getPlatformSegment().setPlatformHub(currentPlatformHub);
            currentPlatformHub.getPlatformSegment().setDirection(Track.Direction.NORTHBOUND);

            currentPlatformHub = station.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub();

            currentPlatformHub.getPlatformSegment().setStation(station);
            currentPlatformHub.getPlatformSegment().setPlatformHub(currentPlatformHub);
            currentPlatformHub.getPlatformSegment().setDirection(Track.Direction.SOUTHBOUND);
        }

        // Get the junctions where the depot is to be connected
        // The depot is to be connected to Santolan station - the northernmost station in the system
        Station depotStation = stations.get(stations.size() - 1);

        Junction inJunction = depotStation.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub()
                .getInConnector();
        Junction outJunction = depotStation.getPlatforms().get(Track.Direction.NORTHBOUND).getPlatformHub()
                .getOutConnector();

        // Get the length of the spur segment which connects to the depot
        int spurDepotSegmentLength = DatabaseQueries.getSpurDepotSegmentLength(databaseInterface,
                trainSystemInformation);

        // Create the depot object, then connect it to the rest of the constructed train line
        Depot depot = new Depot(trainSystem);
        Track.connectDepot(depot, inJunction, outJunction, spurDepotSegmentLength);

        // Set the newly connected depot of the train system
        trainSystem.setDepot(depot);

        // Return the newly created train system
        return trainSystem;
    }

    // Set the MRT-3 system up
    private static TrainSystem setupMRT3(DatabaseInterface databaseInterface,
                                         TrainSystemInformation trainSystemInformation) {
        // Create a train system object
        TrainSystem trainSystem = new TrainSystem(trainSystemInformation);

        // Retrieve all stations
        List<Station> stations = DatabaseQueries.getStations(databaseInterface, trainSystem);

        // Add each to this train system's record
        trainSystem.getStations().addAll(stations);

        // Connect all stations
        for (int stationIndex = 1; stationIndex < stations.size(); stationIndex++) {
            // Get the preceding station
            Station precedingStation = stations.get(stationIndex - 1);

            // Get the current station
            Station currentStation = stations.get(stationIndex);

            // Make sure that the sequence of the stations are correct
            assert currentStation.getSequence() > precedingStation.getSequence() : "Stations are out of sequence";

            // Connect the two together
            Track.connectStations(precedingStation, currentStation, currentStation.getDistanceToPrevious());
        }

        // Get information about the train line's edge segments
        List<EndSegmentsEntity> endSegmentsEntities = DatabaseQueries.getEndSegmentInformation(databaseInterface,
                trainSystemInformation);

        int northEndSegmentLength = endSegmentsEntities.get(1).getLength();
        int southEndSegmentLength = endSegmentsEntities.get(0).getLength();

        // "Close" the loop of the two edge stations
        Station northEdgeStation = stations.get(stations.size() - 1);
        Station southEdgeStation = stations.get(0);

        Track.formNorthLoop(northEdgeStation, northEndSegmentLength);
        Track.formSouthLoop(southEdgeStation, southEndSegmentLength);

        // Mark all segments which belong to a station as belonging to that station
        PlatformHub currentPlatformHub;

        for (Station station : stations) {
            currentPlatformHub = station.getPlatforms().get(Track.Direction.NORTHBOUND).getPlatformHub();

            currentPlatformHub.getPlatformSegment().setStation(station);
            currentPlatformHub.getPlatformSegment().setPlatformHub(currentPlatformHub);
            currentPlatformHub.getPlatformSegment().setDirection(Track.Direction.NORTHBOUND);

            currentPlatformHub = station.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub();

            currentPlatformHub.getPlatformSegment().setStation(station);
            currentPlatformHub.getPlatformSegment().setPlatformHub(currentPlatformHub);
            currentPlatformHub.getPlatformSegment().setDirection(Track.Direction.SOUTHBOUND);
        }

        // Get the junctions where the depot is to be connected
        // The depot is to be connected to North Avenue station - the southernmost station in the system
        Station depotStation = stations.get(0);

        Junction inJunction = depotStation.getPlatforms().get(Track.Direction.NORTHBOUND).getPlatformHub()
                .getInConnector();
        Junction outJunction = depotStation.getPlatforms().get(Track.Direction.SOUTHBOUND).getPlatformHub()
                .getOutConnector();

        // Get the length of the spur segment which connects to the depot
        int spurDepotSegmentLength = DatabaseQueries.getSpurDepotSegmentLength(databaseInterface,
                trainSystemInformation);

        // Create the depot object, then connect it to the rest of the constructed train line
        Depot depot = new Depot(trainSystem);
        Track.connectDepot(depot, inJunction, outJunction, spurDepotSegmentLength);

        // Set the newly connected depot of the train system
        trainSystem.setDepot(depot);

        // Return the newly created train system
        return trainSystem;
    }
}

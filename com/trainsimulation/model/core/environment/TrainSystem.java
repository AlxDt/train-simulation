package com.trainsimulation.model.core.environment;

import com.trainsimulation.model.core.agent.passenger.Passenger;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset.Train;
import com.trainsimulation.model.utility.TrainSystemInformation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    // Get a station by its sequence number
    public static Station getStationBySequenceNumber(Depot trainLine, int sequence) {
        Station station;
        Set<Segment> segmentsDiscovered = new HashSet<>();

        // Traverse each station of the line until:
        // 1) the station with the desired sequence number is found, or
        // 2) all segments have been discovered
        // TODO: Do not assume that the first segment in each junction (unsafe to assume index is always 0)
        Segment segment = trainLine.getPlatformHub().getPlatformSegment().getTo().getOutSegments().get(0);

        // All segments are immediately discovered
        if (addAndCheckSegmentCompleteDiscovery(segmentsDiscovered, segment)) {
            return isSequenceMatchOrNull(segment, sequence);
        }

        // Traverse the system until all segments are discovered
        do {
            // Get the next segment
            while (segment.getStation() == null) {
                segment = segment.getTo().getOutSegments().get(0);

                // If all segments have been discovered at this point, one final check is performed to see if it is the
                // desired station
                // If it isn't, no point on looking as we have already gone in a loop
                if (addAndCheckSegmentCompleteDiscovery(segmentsDiscovered, segment)) {
                    return isSequenceMatchOrNull(segment, sequence);
                }
            }

            // A station is on this segment - check its sequence number
            station = segment.getStation();

            // If this is it, return it
            if (station.getSequence() == sequence) {
                return station;
            } else {
                // Else, move on to the next segment
                segment = segment.getTo().getOutSegments().get(0);

                if (addAndCheckSegmentCompleteDiscovery(segmentsDiscovered, segment)) {
                    return isSequenceMatchOrNull(segment, sequence);
                }
            }
        } while (true);
    }

    // Check if the size of the list of segments discovered has changed or not, after adding a segment
    // If it did not change, it means all segments have been discovered
    private static boolean addAndCheckSegmentCompleteDiscovery(Set<Segment> segmentsDiscovered, Segment segment) {
        int sizeBeforeAdding = segmentsDiscovered.size();
        segmentsDiscovered.add(segment);
        int sizeAfterAdding = segmentsDiscovered.size();

        // Return true if all segments have indeed been discovered
        return sizeBeforeAdding == sizeAfterAdding;
    }

    // Return the station of the segment contains a station and matches the desired sequence,
    // Else, immediately return null
    private static Station isSequenceMatchOrNull(Segment segment, int sequence) {
        if (segment.getStation() != null) {
            Station station = segment.getStation();

            if (station.getSequence() == sequence) {
                return station;
            } else {
                return null;
            }
        } else {
            return null;
        }
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
}

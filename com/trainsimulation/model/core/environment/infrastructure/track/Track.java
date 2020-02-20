package com.trainsimulation.model.core.environment.infrastructure.track;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.Infrastructure;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;

// Defines objects which directly support the movement of trains
public abstract class Track extends Infrastructure {
    public Track(TrainSystem trainSystem) {
        super(trainSystem);
    }

    // Connect the components of a platform hub
    static void setPlatformHub(Junction inConnector, Segment platformSegment,
                               Junction outConnector) {
        connect(inConnector, platformSegment, outConnector);
    }

    // Connect two stations with each other
    public static void connectStations(Station previousStation, Station nextStation, final int distance) {
        // Connect the NB portions of the two stations
        Junction previousStationOutgoingNB
                = previousStation.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub().getOutConnector();
        Junction nextStationIncomingNB
                = nextStation.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub().getInConnector();

        Segment segmentNB = new Segment(previousStation.getTrainSystem(), distance);

        connect(previousStationOutgoingNB, segmentNB, nextStationIncomingNB);

        // Connect the SB portions of the two stations
        Junction previousStationIncomingSB
                = previousStation.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub().getInConnector();
        Junction nextStationOutgoingSB
                = nextStation.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub().getOutConnector();

        Segment segmentSB = new Segment(previousStation.getTrainSystem(), distance);

        connect(nextStationOutgoingSB, segmentSB, previousStationIncomingSB);
    }

    // Connect a depot with a junction
    public static void connectDepot(Depot depot, Junction inJunction, Junction outJunction, int depotSegmentLength) {
        Segment segmentIn = new Segment(depot.getTrainSystem(), depotSegmentLength);
        Segment segmentOut = new Segment(depot.getTrainSystem(), depotSegmentLength);

        connect(outJunction, segmentIn, depot.getPlatformHub().getInConnector());
        connect(depot.getPlatformHub().getOutConnector(), segmentOut, inJunction);
    }

    // Form a northern loop connecting the two platforms of a station
    public static void formNorthLoop(Station station, int northEndSegmentLength) {
        Junction outgoingNB = station.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub().getOutConnector();
        Junction incomingSB = station.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub().getInConnector();

        Junction endJunction = new Junction(station.getTrainSystem());
        endJunction.setEnd(true);

        Segment loopSegmentNB = new Segment(station.getTrainSystem(), northEndSegmentLength);
        Segment loopSegmentSB = new Segment(station.getTrainSystem(), northEndSegmentLength);

        connect(outgoingNB, loopSegmentNB, endJunction);
        connect(endJunction, loopSegmentSB, incomingSB);
    }

    // Form a southern loop connecting the two platforms of a station
    public static void formSouthLoop(Station station, int southEndSegmentLength) {
        Junction outgoingSB = station.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub().getOutConnector();
        Junction incomingNB = station.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub().getInConnector();

        Junction endJunction = new Junction(station.getTrainSystem());
        endJunction.setEnd(true);

        Segment loopSegmentSB = new Segment(station.getTrainSystem(), southEndSegmentLength);
        Segment loopSegmentNB = new Segment(station.getTrainSystem(), southEndSegmentLength);

        connect(outgoingSB, loopSegmentSB, endJunction);
        connect(endJunction, loopSegmentNB, incomingNB);
    }

    // Connect two junctions with a segment
    private static void connect(Junction previousJunction, Segment segment, Junction nextJunction) {
        previousJunction.getOutSegments().add(segment);
        segment.setTo(nextJunction);
    }

    // Enumeration constants for train directions
    public enum Direction {
        NORTHBOUND,
        SOUTHBOUND
    }
}

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
                               Junction outConnector, Direction direction) {
        connect(inConnector, platformSegment, outConnector, direction);
    }

    // Connect two stations with each other
    public static void connectStations(Station previousStation, Station nextStation, final int distance) {
        // Connect the NB portions of the two stations
        PlatformHub previousPlatformHubOutgoingNB
                = previousStation.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub();
        PlatformHub nextPlatformHubIncomingNB
                = nextStation.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub();

        previousPlatformHubOutgoingNB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + Segment.SegmentIdentifier.DELIMITER + previousStation.getName());

        nextPlatformHubIncomingNB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + Segment.SegmentIdentifier.DELIMITER + nextStation.getName());

        Junction previousStationOutgoingNB = previousPlatformHubOutgoingNB.getOutConnector();
        Junction nextStationIncomingNB = nextPlatformHubIncomingNB.getInConnector();

        Segment segmentNB = new Segment(previousStation.getTrainSystem(), distance);

        segmentNB.setDirection(Direction.NORTHBOUND);
        segmentNB.setName(Segment.SegmentIdentifier.MAINLINE + Segment.SegmentIdentifier.DELIMITER
                + previousStation.getName() + " " + Segment.SegmentIdentifier.TO + " " + nextStation.getName());

        connect(previousStationOutgoingNB, segmentNB, nextStationIncomingNB, Direction.NORTHBOUND);

        // Connect the SB portions of the two stations
        PlatformHub previousPlatformHubIncomingSB
                = previousStation.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub();
        PlatformHub nextPlatformHubOutgoingSB
                = nextStation.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub();

        previousPlatformHubIncomingSB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + Segment.SegmentIdentifier.DELIMITER + previousStation.getName());
        nextPlatformHubOutgoingSB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + Segment.SegmentIdentifier.DELIMITER + nextStation.getName());

        Junction previousStationIncomingSB
                = previousPlatformHubIncomingSB.getInConnector();
        Junction nextStationOutgoingSB
                = nextPlatformHubOutgoingSB.getOutConnector();

        Segment segmentSB = new Segment(previousStation.getTrainSystem(), distance);

        segmentSB.setDirection(Direction.SOUTHBOUND);
        segmentSB.setName(Segment.SegmentIdentifier.MAINLINE + Segment.SegmentIdentifier.DELIMITER
                + nextStation.getName() + " " + Segment.SegmentIdentifier.TO + " " + previousStation.getName());

        connect(nextStationOutgoingSB, segmentSB, previousStationIncomingSB, Direction.SOUTHBOUND);
    }

    // Connect a depot with a junction
    public static void connectDepot(Depot depot, Junction inJunction, Junction outJunction, int depotSegmentLength) {
        PlatformHub northboundPlatformHub = depot.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub();
        PlatformHub southboundPlatformHub = depot.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub();

        Segment northboundPlatformSegment = northboundPlatformHub.getPlatformSegment();
        Segment southboundPlatformSegment = southboundPlatformHub.getPlatformSegment();

        northboundPlatformSegment.setDepot(depot);
        northboundPlatformSegment.setDirection(Direction.NORTHBOUND);
        northboundPlatformSegment.setName(Segment.SegmentIdentifier.DEPOT
                + Segment.SegmentIdentifier.DELIMITER + Segment.SegmentIdentifier.DEPOT_OUT);

        southboundPlatformSegment.setDepot(depot);
        southboundPlatformSegment.setDirection(Direction.SOUTHBOUND);
        southboundPlatformSegment.setName(Segment.SegmentIdentifier.DEPOT
                + Segment.SegmentIdentifier.DELIMITER + Segment.SegmentIdentifier.DEPOT_IN);

        Segment segmentIn = new Segment(depot.getTrainSystem(), depotSegmentLength);

        segmentIn.setDirection(Direction.DEPOT_IN);
        segmentIn.setName(Segment.SegmentIdentifier.DEPOT + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.DEPOT_IN);

        Segment segmentOut = new Segment(depot.getTrainSystem(), depotSegmentLength);

        segmentOut.setDirection(Direction.DEPOT_OUT);
        segmentOut.setName(Segment.SegmentIdentifier.DEPOT + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.DEPOT_OUT);

        connect(outJunction, segmentIn, southboundPlatformHub.getInConnector(), Direction.DEPOT_IN);
        connect(northboundPlatformHub.getOutConnector(), segmentOut, inJunction, Direction.DEPOT_OUT);
    }

    // Form a northern loop connecting the two platforms of a station
    public static void formNorthLoop(Station station, int northEndSegmentLength) {
        Junction outgoingNB = station.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub().getOutConnector();
        Junction incomingSB = station.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub().getInConnector();

        Junction endJunction = new Junction(station.getTrainSystem());
        endJunction.setEnd(true);

        Segment loopSegmentNB = new Segment(station.getTrainSystem(), northEndSegmentLength);

        loopSegmentNB.setDirection(Direction.NORTHBOUND);
        loopSegmentNB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_OUT + " " + station.getName());

        Segment loopSegmentSB = new Segment(station.getTrainSystem(), northEndSegmentLength);

        loopSegmentSB.setDirection(Direction.SOUTHBOUND);
        loopSegmentSB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_IN + " " + station.getName());

        connect(outgoingNB, loopSegmentNB, endJunction, Direction.NORTHBOUND);
        connect(endJunction, loopSegmentSB, incomingSB, Direction.SOUTHBOUND);
    }

    // Form a southern loop connecting the two platforms of a station
    public static void formSouthLoop(Station station, int southEndSegmentLength) {
        Junction outgoingSB = station.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub().getOutConnector();
        Junction incomingNB = station.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub().getInConnector();

        Junction endJunction = new Junction(station.getTrainSystem());
        endJunction.setEnd(true);

        Segment loopSegmentSB = new Segment(station.getTrainSystem(), southEndSegmentLength);

        loopSegmentSB.setDirection(Direction.SOUTHBOUND);
        loopSegmentSB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_OUT + " " + station.getName());

        Segment loopSegmentNB = new Segment(station.getTrainSystem(), southEndSegmentLength);

        loopSegmentNB.setDirection(Direction.NORTHBOUND);
        loopSegmentNB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_IN + " " + station.getName());

        connect(outgoingSB, loopSegmentSB, endJunction, Direction.SOUTHBOUND);
        connect(endJunction, loopSegmentNB, incomingNB, Direction.NORTHBOUND);
    }

    // Connect two junctions with a segment
    private static void connect(Junction previousJunction, Segment segment, Junction nextJunction,
                                Direction direction) {
        previousJunction.getOutSegments().add(segment);
//        previousJunction.insertOutSegment(direction, segment);

        segment.setFrom(previousJunction);
        segment.setTo(nextJunction);
    }

    // Enumeration constants for train directions
    public enum Direction {
        NORTHBOUND,
        SOUTHBOUND,
        DEPOT_IN,
        DEPOT_OUT,
        BRANCH
    }
}

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
        PlatformHub previousPlatformHubOutgoingNB
                = previousStation.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub();
        PlatformHub nextPlatformHubIncomingNB
                = nextStation.getPlatforms().get(Direction.NORTHBOUND).getPlatformHub();

        previousPlatformHubOutgoingNB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + ":" + previousStation.getName());
        nextPlatformHubIncomingNB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + ":" + nextStation.getName());

        Junction previousStationOutgoingNB = previousPlatformHubOutgoingNB.getOutConnector();
        Junction nextStationIncomingNB = nextPlatformHubIncomingNB.getInConnector();

        Segment segmentNB = new Segment(previousStation.getTrainSystem(), distance);
        segmentNB.setName(Segment.SegmentIdentifier.MAINLINE
                + ":" + previousStation.getName() + " to " + nextStation.getName());

        connect(previousStationOutgoingNB, segmentNB, nextStationIncomingNB);

        // Connect the SB portions of the two stations
        PlatformHub previousPlatformHubIncomingSB
                = previousStation.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub();
        PlatformHub nextPlatformHubOutgoingSB
                = nextStation.getPlatforms().get(Direction.SOUTHBOUND).getPlatformHub();

        previousPlatformHubIncomingSB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + ":" + previousStation.getName());
        nextPlatformHubOutgoingSB.getPlatformSegment().setName(Segment.SegmentIdentifier.STATION
                + ":" + nextStation.getName());

        Junction previousStationIncomingSB
                = previousPlatformHubIncomingSB.getInConnector();
        Junction nextStationOutgoingSB
                = nextPlatformHubOutgoingSB.getOutConnector();

        Segment segmentSB = new Segment(previousStation.getTrainSystem(), distance);
        segmentSB.setName(Segment.SegmentIdentifier.MAINLINE
                + ":" + nextStation.getName() + " to " + previousStation.getName());

        connect(nextStationOutgoingSB, segmentSB, previousStationIncomingSB);
    }

    // Connect a depot with a junction
    public static void connectDepot(Depot depot, Junction inJunction, Junction outJunction, int depotSegmentLength) {
        depot.getPlatformHub().getPlatformSegment().setName(Segment.SegmentIdentifier.DEPOT
                + Segment.SegmentIdentifier.DELIMITER + Segment.SegmentIdentifier.DEPOT_OUT);

        Segment segmentIn = new Segment(depot.getTrainSystem(), depotSegmentLength);
        segmentIn.setName(Segment.SegmentIdentifier.DEPOT + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.DEPOT_IN);

        Segment segmentOut = new Segment(depot.getTrainSystem(), depotSegmentLength);
        segmentOut.setName(Segment.SegmentIdentifier.DEPOT + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.DEPOT_OUT);

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
        loopSegmentNB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_OUT + " " + station.getName());

        Segment loopSegmentSB = new Segment(station.getTrainSystem(), northEndSegmentLength);
        loopSegmentSB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_IN + " " + station.getName());

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
        loopSegmentSB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_OUT + " " + station.getName());

        Segment loopSegmentNB = new Segment(station.getTrainSystem(), southEndSegmentLength);
        loopSegmentNB.setName(Segment.SegmentIdentifier.LOOP + Segment.SegmentIdentifier.DELIMITER
                + Segment.SegmentIdentifier.LOOP_IN + " " + station.getName());

        connect(outgoingSB, loopSegmentSB, endJunction);
        connect(endJunction, loopSegmentNB, incomingNB);
    }

    // Connect two junctions with a segment
    private static void connect(Junction previousJunction, Segment segment, Junction nextJunction) {
        previousJunction.getOutSegments().add(segment);

        segment.setFrom(previousJunction);
        segment.setTo(nextJunction);
    }

    // Enumeration constants for train directions
    public enum Direction {
        NORTHBOUND,
        SOUTHBOUND
    }
}

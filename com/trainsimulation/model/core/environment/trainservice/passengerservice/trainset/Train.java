package com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset;

import com.trainsimulation.controller.Main;
import com.trainsimulation.model.core.agent.Agent;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.db.DatabaseQueries;
import com.trainsimulation.model.db.entity.TrainCarriagesEntity;
import com.trainsimulation.model.db.entity.TrainsEntity;
import com.trainsimulation.model.simulator.SimulationTime;
import com.trainsimulation.model.utility.TrainMovement;

import java.util.LinkedList;
import java.util.List;

// Vehicles composed of multiple carriages used to transport passengers
public class Train extends TrainSet implements Agent {
    // Denotes the gap between each carriages (in meters)
    private static final double CARRIAGE_GAP = 1.0;

    // Contains the carriages this train is composed of
    private final LinkedList<TrainCarriage> trainCarriages;

    // Contains the variables relevant to the train's movement
    private final TrainMovement trainMovement;

    public Train(TrainSystem trainSystem, TrainsEntity trainsEntity) {
        super(trainSystem, trainsEntity.getId());

        this.trainCarriages = new LinkedList<>();

        // Get the carriages associated with this train
        List<TrainCarriagesEntity> trainCarriagesEntities = DatabaseQueries.getTrainCarriages(
                Main.SIMULATOR.getDatabaseInterface(), this);

        // Add the train carriages
        Integer maxVelocity = null;
        Integer waitingTime = null;

        // There should always be carriages associated with this train
        assert trainCarriagesEntities.size() != 0 : "No train carriages associated with this train";

        for (TrainCarriagesEntity trainCarriagesEntity : trainCarriagesEntities) {
            int quantity = trainCarriagesEntity.getQuantity();

            for (int count = 1; count <= quantity; count++) {
                TrainCarriage trainCarriage = new TrainCarriage(trainSystem, trainCarriagesEntity, this);

                // Add the maximum velocity and waiting times
                if (maxVelocity == null && waitingTime == null) {
                    maxVelocity = (int) trainCarriagesEntity.getCarriageClassesByCarriageClass().getMaxVelocity();
                    waitingTime = (int) trainCarriagesEntity.getTrainsByTrain().getWaitingTime();
                }

                this.trainCarriages.add(trainCarriage);
            }
        }

        this.trainMovement = new TrainMovement(maxVelocity, waitingTime, this);
    }

    public LinkedList<TrainCarriage> getTrainCarriages() {
        return trainCarriages;
    }

    public TrainCarriage getHead() {
        return trainCarriages.getFirst();
    }

    public TrainCarriage getTail() {
        return trainCarriages.getLast();
    }

    public TrainMovement getTrainMovement() {
        return trainMovement;
    }

    @Override
    public void run() {
        try {
            // Originate the train from the depot belonging to the train system where this train belongs to
            Depot depot = this.getTrainSystem().getDepot();
            this.setTrainAtSegment(depot.getPlatformHub().getPlatformSegment());

            // The first action was to stop - as it was in a depot
            TrainMovement.TrainAction trainAction = TrainMovement.TrainAction.STOP_FOR_STATION;

            // Exit the depot
            while (true) {
                // Move until it is commanded otherwise (when it stops for a station or for another train)
                do {
                    // Take note of the action command
                    trainAction = this.trainMovement.move(trainAction);

                    // Pause the thread
                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);
//                    System.out.println("moving");
                } while (trainAction != TrainMovement.TrainAction.STOP_FOR_STATION);

                // Wait in the station for the specified amount of time
                while (this.trainMovement.waitAtStation()) {
                    // Pause the thread
                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);
//                    System.out.println("waiting");
                }
//
//                System.out.println("waited");
            }

//            // Clear this segment and take note of the excess clearance
//            // TODO: Add/remove train queue should be moved to segment clearance method
//            segment.getTrainQueue().add(this);
//            this.metersElapsed = clearSegment(this.metersElapsed, segment.getLength(), METERS_PER_SECOND);
//            segment.getTrainQueue().remove();
//
//            // Begin traversing the system
//            while (true) {
//                // While the current segment does not belong to a station, keep going to the next segment
//                while (segment.getStation() == null) {
//                    // TODO: Draw train position
//                    GraphicsController.requestDraw(CANVAS.getGraphicsContext2D(), depot);
//
//                    // Pause the thread
//                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);
//
//                    // Clear this segment
//                    segment.getTrainQueue().add(this);
//                    this.metersElapsed = clearSegment(this.metersElapsed, segment.getLength(), METERS_PER_SECOND);
//                    segment.getTrainQueue().remove();
//
//                    // Go to the next segment
//                    segment = segment.getTo().getOutSegments().get(0);
//                }
//
//                // The train is now in a station - wait for the specified amount of time
//                // Only this train will have access to this station at this time
//                segment.getPlatformHub().getInConnector().getSignal().acquire();
//
//                System.out.println(segment.getStation().getName());
//
//                // TODO: Draw train position
//                GraphicsController.requestDraw(CANVAS.getGraphicsContext2D(), depot);
//
//                for (int waitedTime = 0; waitedTime < WAITING_TIME_SECONDS; waitedTime++) {
//                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);
//                }
//
//                // Following trains may now enter this station
//                segment.getPlatformHub().getInConnector().getSignal().release();
//
//                // Exit the station
//                segment = segment.getTo().getOutSegments().get(0);
//            }

//            // Test if a train goes around in a perfect loop
//            Station current = stations.get(stations.size() - 1);
//            String direction = "SB";
//
//            Segment segment;
//
//            while (true) {
//                current.getPlatforms().get(direction).getPlatformHub().getInConnector().getSignal().acquire();
//                System.out.println("[" + Thread.currentThread().getName() + " @ " + current.getName() + "] " + direction);
//
////                    System.out.print("\tWaiting...");
//
//                for (int count = 30; count >= 1; count--) {
////                        System.out.print(count + "..");
//                    Thread.sleep(25);
//                }
//
////                    System.out.println();
////                    System.out.println("\tDeparted");
//
//                System.out.println("waiting");
//                Thread.sleep(3000);
//                System.out.println("waited");
//
//                current.getPlatforms().get(direction).getPlatformHub().getInConnector().getSignal().release();
//                segment = current.getPlatforms().get(direction).getPlatformHub().getOutConnector().getOutSegments().get(0);
//
//                do {
//                    metersElapsed = 0;
//
////                        System.out.print(">Traveling " + direction + " along a " + segment.getLength() + " m long segment...");
//                    while (metersElapsed / segment.getLength() <= 1.0) {
////                    System.out.println("Completed " + metersElapsed + " m of " + segment.getLength() + " length of segment "
////                            + "(" + metersElapsed / segment.getLength() * 100.0 + "%)");
////                            System.out.print(".");
//                        Thread.sleep(25);
//
//                        metersElapsed += METERS_PER_SECOND;
//                    }
//
////                        System.out.println();
//
//                    segment = segment.getTo().getOutSegments().get(0);
//                    current = segment.getStation();
//
//                    if (current == null) {
//                        if (direction.equals("NB")) {
//                            direction = "SB";
//                        } else {
//                            direction = "NB";
//                        }
//
////                            System.out.println(">End of line; switching to " + direction);
//                    }
//                } while (current == null);
//            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

//    // Clear a single segment then return the excess clearance
//    // TODO: Move to TrainMovement class
//    private double clearSegment(double initialClearance, double segmentLength, final double velocity)
//            throws InterruptedException {
//        this.metersElapsed = initialClearance;
//
//        while (this.metersElapsed / segmentLength <= 1.0) {
//            // TODO: Draw train position
//            GraphicsController.requestDraw(CANVAS.getGraphicsContext2D(), depot);
//
//            //System.out.println(this.metersElapsed);
//
//            // Pause the thread
//            Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);
//
//            this.metersElapsed += velocity;
//        }
//
//        // Take note of the excess clearance
//        return metersElapsed - segmentLength;
//    }

    // Activate a train
    public void activate(List<Train> activeTrains) {
        // Add it to the list of active trains
        activeTrains.add(this);

        // Finally, start the train thread
        new Thread(this).start();
    }

    // TODO: Move to train movement
    // Set this train to a segment
    public void setTrainAtSegment(Segment segment) {
        // Set in such a way that the train is just about to clear this segment
        final double headClearance = segment.getLength();
        double carriageOffset = 0.0;

        // Compute for the location of each train carriage
        for (TrainCarriage trainCarriage : this.trainCarriages) {
            // Add this carriage to this segment
            segment.getTrainQueue().add(trainCarriage);

            // Set the clearance of this segment
            trainCarriage.getTrainCarriageLocation().setSegmentClearance(headClearance - carriageOffset);

            // Set the location of this carriage to this segment
            trainCarriage.getTrainCarriageLocation().setSegmentLocation(segment);

            // Increment the offset
            carriageOffset += trainCarriage.getLength() + CARRIAGE_GAP;
        }
    }
}

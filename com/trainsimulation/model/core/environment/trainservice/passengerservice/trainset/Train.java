package com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset;

import com.trainsimulation.controller.Main;
import com.trainsimulation.controller.screen.MainScreenController;
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

            // Keep track of whether the train has entered its first station from the depot
            boolean hasEnteredStationFromDepot = false;

            // Exit the depot
            while (true) {
                // Move until it is commanded otherwise (when it stops for a station or to avoid colliding with another
                // train)
                do {
                    // Take note of the action command
                    trainAction = this.trainMovement.move(trainAction);

                    // Pause the thread
                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);
//                    System.out.println("moving");
                } while (trainAction != TrainMovement.TrainAction.STOP_FOR_STATION);

                // If it hasn't been noted yet, the train has now entered its first station from the depot
                if (!hasEnteredStationFromDepot) {
                    hasEnteredStationFromDepot = true;

                    // Tell the GUI to enable the add train button again
                    MainScreenController.ARM_ADD_TRAIN_BUTTON.release();
                }

                // Wait in the station for the specified amount of time
                while (this.trainMovement.waitAtStation()) {
                    // Pause the thread
                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);
//                    System.out.println("waiting");
                }
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

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
            segment.getTrainQueue().insertTrainCarriage(trainCarriage);

            // Set the clearance of this segment
            trainCarriage.getTrainCarriageLocation().setSegmentClearance(headClearance - carriageOffset);

            // Set the location of this carriage to this segment
            trainCarriage.getTrainCarriageLocation().setSegmentLocation(segment);

            // Increment the offset
            carriageOffset += trainCarriage.getLength() + CARRIAGE_GAP;
        }
    }
}

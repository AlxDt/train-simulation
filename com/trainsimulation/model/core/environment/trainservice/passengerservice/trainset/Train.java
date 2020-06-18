package com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset;

import com.trainsimulation.controller.Main;
import com.trainsimulation.controller.screen.MainScreenController;
import com.trainsimulation.model.core.agent.Agent;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.infrastructure.track.Segment;
import com.trainsimulation.model.core.environment.infrastructure.track.Track;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.property.TrainProperty;
import com.trainsimulation.model.db.DatabaseQueries;
import com.trainsimulation.model.db.entity.TrainCarriagesEntity;
import com.trainsimulation.model.db.entity.TrainsEntity;
import com.trainsimulation.model.simulator.SimulationTime;
import com.trainsimulation.model.simulator.Simulator;
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
    private TrainMovement trainMovement;

    // Contains a summarized representation of this train (for table tracking purposes)
    private TrainProperty trainProperty;

    public Train(TrainSystem trainSystem, TrainsEntity trainsEntity) {
        super(trainSystem, trainsEntity.getId());

        this.trainCarriages = new LinkedList<>();

        resetTrain(trainSystem);
    }

    private void resetTrain(TrainSystem trainSystem) {
        // Remove all train carriages, if any
        this.trainCarriages.clear();

        // Get the carriages associated with this train
        List<TrainCarriagesEntity> trainCarriagesEntities = DatabaseQueries.getTrainCarriages(
                Main.simulator.getDatabaseInterface(), this);

        // Add the train carriages
        Integer maxVelocity = null;
        Double deceleration = null;

        // There should always be carriages associated with this train
        assert trainCarriagesEntities.size() != 0 : "No train carriages associated with this train";

        for (TrainCarriagesEntity trainCarriagesEntity : trainCarriagesEntities) {
            int quantity = trainCarriagesEntity.getQuantity();

            for (int count = 1; count <= quantity; count++) {
                TrainCarriage trainCarriage = new TrainCarriage(trainSystem, trainCarriagesEntity, this);

                // Add the maximum velocity and waiting times
                if (maxVelocity == null && deceleration == null) {
                    maxVelocity = (int) trainCarriagesEntity.getCarriageClassesByCarriageClass().getMaxVelocity();
                    deceleration = (double) trainCarriagesEntity.getCarriageClassesByCarriageClass().getDeceleration();
                }

                this.trainCarriages.add(trainCarriage);
            }
        }

        this.trainMovement = new TrainMovement(maxVelocity, deceleration, this);
        this.trainProperty = new TrainProperty(this);
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

    public void setTrainMovement(TrainMovement trainMovement) {
        this.trainMovement = trainMovement;
    }

    public TrainProperty getTrainProperty() {
        return trainProperty;
    }

    public void setTrainProperty(TrainProperty trainProperty) {
        this.trainProperty = trainProperty;
    }

    @Override
    public void run() {
        try {
            // Originate the train from the depot belonging to the train system where this train belongs to
            Depot depot = this.getTrainSystem().getDepot();
            this.trainMovement.setTrainAtSegment(depot.getPlatforms().get(Track.Direction.NORTHBOUND)
                    .getPlatformHub().getPlatformSegment(), CARRIAGE_GAP);

            // Take note of the actions made
            TrainMovement.TrainAction trainAction;

            // Exit the depot, then keep moving until the simulation is done
            // TODO: Trains should also go home when told to, or when it's past operating hours
            while (!Simulator.getDone().get()) {
                // Move until it is commanded otherwise (when it stops for a station or to avoid colliding with another
                // train, or when it stops because of a signal), or until the simulation is done
                do {
                    // Take note of the action command
                    trainAction = this.trainMovement.move();

                    // Update the summary
                    this.trainProperty.updateTrainProperty(this.identifier, this.trainMovement, this.trainCarriages
                            .getFirst());

                    // Pause the thread
                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS.get());
                } while (trainAction == TrainMovement.TrainAction.PROCEED && !Simulator.getDone().get());

                // Do the specified actions (headway and signal stops do not have any explicit actions)
                switch (trainAction) {
                    case END_STOP:
                        // Wait in the end for the specified amount of time
                        while (this.trainMovement.waitAtEnd() && !Simulator.getDone().get()) {
                            // Pause the thread
                            Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS.get());
                        }

                        break;
                    case STATION_STOP:
                        // Wait in the station for the specified amount of time
                        while (this.trainMovement.waitAtStation() && !Simulator.getDone().get()) {
                            // If the train has been deactivated while waiting at a station, this will serve as the
                            // train's final station stop
                            // TODO: Maybe extend the waiting time to account for passengers disembarking?
                            if (!this.getTrainMovement().isActive()) {
                                this.getTrainMovement().setDisembarkedWhenRemoved(true);
                            }

                            // Pause the thread
                            Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS.get());
                        }

                        break;
                }

                // Finally, check if the train has reached the home depot
                // If it has, then the train is now ready to despawn
                if (!this.getTrainMovement().isActive() && trainAction == TrainMovement.TrainAction.DEPOT_STOP) {
                    this.pullOut(this.getTrainSystem().getActiveTrains(), this.getTrainSystem().getInactiveTrains());

                    break;
                }

                // Update the summary
                // TODO: Include train action in properties
                this.trainProperty.updateTrainProperty(this.identifier, this.trainMovement, this.trainCarriages
                        .getFirst());
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    // Deploy a train
    public void deploy(List<Train> activeTrains, List<Train> inactiveTrains) {
        // Remove this train from the list of inactive trains
        inactiveTrains.remove(this);

        // Add this train to the list of active trains
        activeTrains.add(this);

        // Set the train status to active
        this.getTrainMovement().setActive(true);

        // Finally, start the train thread
        new Thread(this).start();
    }

    // Pull a train out of the system
    public void pullOut(List<Train> activeTrains, List<Train> inactiveTrains) {
        // Remove this train from the list of active trains
        activeTrains.remove(this);

        // Add this train to the list of deactivated trains
        inactiveTrains.add(this);

        // Clear the segment(s) where this train used to be
        // Release the signals for these segments as well
        Segment segment;

        for (TrainCarriage trainCarriage : this.trainCarriages) {
            segment = trainCarriage.getTrainCarriageLocation().getSegmentLocation();

            segment.getTrainQueue().clearTrainQueue();
            segment.getFrom().getSignal().release();
        }

        // Reset the train to its default settings
        this.resetTrain(this.getTrainSystem());

        // Enable the insert train button
        MainScreenController.ARM_ADD_TRAIN_BUTTON.release();

        // Update the UI elements
        Main.mainScreenController.requestUpdateUI(MainScreenController.getActiveSimulationContext().getTrainSystem(),
                false);
    }
}

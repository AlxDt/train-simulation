package com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset;

import com.trainsimulation.controller.Main;
import com.trainsimulation.controller.screen.MainScreenController;
import com.trainsimulation.model.core.agent.Agent;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.core.environment.trainservice.maintenance.Depot;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.property.TrainProperty;
import com.trainsimulation.model.core.environment.trainservice.passengerservice.stationset.Station;
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
    private final TrainMovement trainMovement;

    // Contains a summarized representation of this train (for table tracking purposes)
    private final TrainProperty trainProperty;

    public Train(TrainSystem trainSystem, TrainsEntity trainsEntity) {
        super(trainSystem, trainsEntity.getId());

        this.trainCarriages = new LinkedList<>();

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

    public TrainProperty getTrainProperty() {
        return trainProperty;
    }

    @Override
    public void run() {
        try {
            // Originate the train from the depot belonging to the train system where this train belongs to
            Depot depot = this.getTrainSystem().getDepot();
            this.trainMovement.setTrainAtSegment(depot.getPlatformHub().getPlatformSegment(), CARRIAGE_GAP);

            // Take note of the actions made
            TrainMovement.TrainAction trainAction;

            // Keep track of whether the train has entered its first station from the depot
            boolean hasEnteredStationFromDepot = false;

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
                        // If it hasn't been noted yet, the train has now entered its first station from the depot
                        if (!hasEnteredStationFromDepot) {
                            hasEnteredStationFromDepot = true;

                            // Tell the GUI to enable the add train button again
                            MainScreenController.ARM_ADD_TRAIN_BUTTON.release();
                        }

                        // Wait in the station for the specified amount of time
                        while (this.trainMovement.waitAtStation() && !Simulator.getDone().get()) {
                            // Pause the thread
                            Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS.get());
                        }

                        break;
                }

                // Update the summary
                this.trainProperty.updateTrainProperty(this.identifier, this.trainMovement, this.trainCarriages
                        .getFirst());
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
}

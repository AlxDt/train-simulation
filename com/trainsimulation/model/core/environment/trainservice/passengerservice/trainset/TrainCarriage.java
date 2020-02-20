package com.trainsimulation.model.core.environment.trainservice.passengerservice.trainset;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.db.entity.TrainCarriagesEntity;
import com.trainsimulation.model.utility.Capacity;
import com.trainsimulation.model.utility.PassengerDemographic;
import com.trainsimulation.model.utility.TrainCarriageLocation;

import java.util.List;

// Vehicles which compose a complete train
public class TrainCarriage extends TrainSet {
    // Denotes the number of passengers the carriage can hold
    private final Capacity carriageCapacity;

    // Denotes the class name of the carriage
    private final String className;

    // Denotes the length of the train carriage
    private final double length;

    // Denotes the list of passenger demographics allowed on the train carriage
    private final List<PassengerDemographic> passengerWhitelist;

    // Denotes the parent of this train carriage
    private final Train parentTrain;

    // Denotes information about the location of this carriage
    private final TrainCarriageLocation trainCarriageLocation;

    public TrainCarriage(TrainSystem trainSystem, TrainCarriagesEntity trainCarriagesEntity, Train trainParent) {
        super(trainSystem, trainCarriagesEntity.getId());

        this.carriageCapacity = new Capacity(trainCarriagesEntity.getCarriageClassesByCarriageClass().getCapacity());
        this.className = trainCarriagesEntity.getCarriageClassesByCarriageClass().getClassName();
        this.length = trainCarriagesEntity.getCarriageClassesByCarriageClass().getLength();
        this.parentTrain = trainParent;
        this.trainCarriageLocation = new TrainCarriageLocation();

        // TODO: Fix nulls
        this.passengerWhitelist = null;
    }

    public Capacity getCarriageCapacity() {
        return carriageCapacity;
    }

    public String getClassName() {
        return className;
    }

    public double getLength() {
        return length;
    }

    public List<PassengerDemographic> getPassengerWhitelist() {
        return passengerWhitelist;
    }

    public Train getParentTrain() {
        return parentTrain;
    }

    public TrainCarriageLocation getTrainCarriageLocation() {
        return trainCarriageLocation;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TrainCarriage) {
            return this.identifier == ((TrainCarriage) object).identifier;
        } else {
            return false;
        }
    }
}

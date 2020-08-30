package com.trainsimulation.model.utility;

import com.trainsimulation.model.db.entity.TrainSystemsEntity;
import com.trainsimulation.model.simulator.SimulationTime;

import java.util.Objects;

// Used to identify which train system a simulation component is part of
public class TrainSystemInformation {
    // Denotes the name of the train system
    private final String name;

    // Denotes the assigned color of the train system (for visualization purposes)
    private final String color;

    // Denotes the schedule of operations for this train system
    private final Schedule schedule;

    public TrainSystemInformation(String trainSystem, String color, SimulationTime startTime, SimulationTime endTime) {
        this.name = trainSystem;
        this.color = color;

        this.schedule = new Schedule(startTime, endTime);
    }

    public TrainSystemInformation(TrainSystemsEntity trainSystemsEntity) {
        this.name = trainSystemsEntity.getName();
        this.color = trainSystemsEntity.getColor();

        this.schedule = new Schedule(trainSystemsEntity.getSchedulesBySchedule());
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrainSystemInformation that = (TrainSystemInformation) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

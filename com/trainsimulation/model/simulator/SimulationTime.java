package com.trainsimulation.model.simulator;

import java.time.LocalTime;

// Represents an object signifying the time in the simulation
public class SimulationTime {
    // The time a thread has to pause (in milliseconds) in order to make the pace of the simulation conducive to
    // visualization
    public static final int SLEEP_TIME_MILLISECONDS = 10;

    private LocalTime time;

    public SimulationTime(SimulationTime simulationTime) {
        this.time = LocalTime.of(simulationTime.getTime().getHour(), simulationTime.getTime().getMinute(),
                simulationTime.getTime().getSecond());
    }

    public SimulationTime(int hour, int minute, int second) {
        this.time = LocalTime.of(hour, minute, second);
    }

    // Return true unless the given time is ahead of the given ending time
    public boolean isTimeBeforeOrDuring(SimulationTime endingTime) {
        return this.time.compareTo(endingTime.getTime()) <= 0;
    }

    // Increment the time by one second
    public void tick() {
        final long INCREMENT_COUNT = 1L;

        this.setTime(this.time.plusSeconds(INCREMENT_COUNT));
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }
}

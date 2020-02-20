package com.trainsimulation.model.simulator;

import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.db.DatabaseInterface;
import com.trainsimulation.model.simulator.setup.EnvironmentSetup;

import java.util.ArrayList;
import java.util.List;

// The simulator has total control over the aspects of the train simulation
public class Simulator {
    // Contains the database interfacing methods of the simulator
    private final DatabaseInterface databaseInterface;

    // Contains all the train systems in the simulation
    private final List<TrainSystem> trainSystems;

    // Stores the current time in the simulation
    private SimulationTime time;

    // Stores the time when the simulation will end
    private SimulationTime endTime;

    public Simulator(SimulationTime time) throws Throwable {
        this.databaseInterface = new DatabaseInterface();

        this.time = time;

        this.trainSystems = new ArrayList<>();
    }

    public Simulator() throws Throwable {
        this.databaseInterface = new DatabaseInterface();

        this.trainSystems = new ArrayList<>();
    }

    public DatabaseInterface getDatabaseInterface() {
        return databaseInterface;
    }

    public SimulationTime getTime() {
        return time;
    }

    public void setTime(SimulationTime time) {
        this.time = time;
    }

    public List<TrainSystem> getTrainSystems() {
        return trainSystems;
    }

    public SimulationTime getEndTime() {
        return endTime;
    }

    public void setEndTime(SimulationTime endTime) {
        this.endTime = endTime;
    }

    // Set the simulation up with all its environments, agents, and attributes
    public void setup(SimulationTime startTime, SimulationTime endTime) {
        // Prepare the time at the start of the simulation
        this.time = startTime;
        this.endTime = endTime;

        // Prepare the train systems
        List<TrainSystem> trainSystems = EnvironmentSetup.setup(databaseInterface);

        // Then have the simulation take note of them
        // But not before clearing the current array of train systems, so the setup could be run indefinitely
        this.trainSystems.clear();
        this.trainSystems.addAll(trainSystems);
    }

    // Start the simulation and keep it running until the given ending time
    public void start() throws InterruptedException {
        // From the starting time until the ending time, increment the time of the simulation
        while (this.time.isTimeBeforeOrDuring(this.endTime)) {
            // TODO: Redraw the graphics corresponding to the current simulation state

            // Pause this simulation thread for a brief amount of time so it could be followed at a pace conducive to
            // visualization
            Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS);

            // Increment (tick) the clock
            this.time.tick();
        }

        // TODO: After this point, tell all trains to head back to the depot
    }

    // TODO: Implement simulation playing/pausing logic
    public void togglePlayPause() {

    }
}

package com.trainsimulation.model.simulator;

import com.trainsimulation.controller.Main;
import com.trainsimulation.model.core.environment.TrainSystem;
import com.trainsimulation.model.db.DatabaseInterface;
import com.trainsimulation.model.simulator.setup.EnvironmentSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// The simulator has total control over the aspects of the train simulation
public class Simulator {
    // Denotes whether the simulation has stated yet or not
    private static final AtomicBoolean started = new AtomicBoolean(false);

    // Denotes whether the simulation is done or not
    private static final AtomicBoolean done = new AtomicBoolean(false);

    // Contains the database interfacing methods of the simulator
    private final DatabaseInterface databaseInterface;

    // Contains all the train systems in the simulation
    private final List<TrainSystem> trainSystems;

    // Stores the current time in the simulation
    private SimulationTime time;

    // Stores the time when the simulation will end
    private SimulationTime endTime;

    public Simulator() throws Throwable {
        this.databaseInterface = new DatabaseInterface();
        this.trainSystems = new ArrayList<>();
    }

    public static AtomicBoolean getStarted() {
        return Simulator.started;
    }

    public static AtomicBoolean getDone() {
        return Simulator.done;
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
    public void start() {
        // Signal to everyone that the simulator has been started
        Simulator.started.set(true);

        // Run this on a thread so it won't choke the JavaFX UI thread
        new Thread(() -> {
            // From the starting time until the ending time, increment the time of the simulation
            while (this.time.isTimeBeforeOrDuring(this.endTime)) {
                // Redraw the updated time
                Main.mainScreenController.requestUpdateSimulationTime(this.time);

                // Pause this simulation thread for a brief amount of time so it could be followed at a pace conducive
                // to visualization
                try {
                    Thread.sleep(SimulationTime.SLEEP_TIME_MILLISECONDS.get());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Increment (tick) the clock
                this.time.tick();
            }

            // Once the simulation time stops, stop all the threads
            Simulator.done.set(true);

            // Then tell the UI thread to disable all buttons
            Main.mainScreenController.requestDisableButtons();
        }).start();
    }

    // TODO: Implement simulation playing/pausing logic
    public void togglePlayPause() {

    }
}
